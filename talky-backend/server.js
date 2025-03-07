const express = require("express");
const multer = require("multer");
const { S3Client, PutObjectCommand } = require("@aws-sdk/client-s3");
const pool = require("./database");
const dotenv = require("dotenv");
const cors = require("cors");
const Redis = require("ioredis");
const redis = new Redis({
  host: "127.0.0.1",
  port: 6380,
  maxRetriesPerRequest: null,  // Prevents max retry errors
  enableReadyCheck: false,  // Improves reconnection time
  retryStrategy: (times) => Math.min(times * 50, 2000), // Retry logic
  reconnectOnError: (err) => {
    console.error("🔴 Redis Connection Error:", err);
    return true; // Auto-reconnect
  }
});

// Redis Event Listeners
redis.on("connect", () => console.log("✅ Connected to Redis"));
redis.on("error", (err) => console.error("❌ Redis Error:", err));
redis.on("end", () => console.warn("⚠️ Redis connection closed, retrying..."));



dotenv.config();
const app = express();
app.use(cors());
app.use(express.json());

// AWS S3 Config
const s3 = new S3Client({
  region: process.env.AWS_REGION,
  credentials: {
    accessKeyId: process.env.AWS_ACCESS_KEY,
    secretAccessKey: process.env.AWS_SECRET_KEY,
  },
});

// Multer Setup for Image Upload
const storage = multer.memoryStorage();
const upload = multer({ storage: storage });

// 🚀 Save or Update User (When signing up via Firebase)
app.post("/save-user", async (req, res) => {
  try {
    console.log("\n📩 Received request at /save-user:", req.body);

    const { uid, username, phoneNumber } = req.body;

    if (!uid || !phoneNumber) {
      console.error("❌ Missing required fields:", { uid, username, phoneNumber });
      return res.status(400).json({ error: "UID and phone number are required" });
    }

    console.log("🔍 Checking if user exists with UID:", uid);

    const existingUser = await pool.query("SELECT id FROM users WHERE id = $1", [uid]);

    if (existingUser.rowCount === 0) {
      console.log("🆕 Creating new user...");
      await pool.query(
        "INSERT INTO users (id, username, phone_number, profile_pic) VALUES ($1, $2, $3, NULL)",
        [uid, username || `User_${uid}`, phoneNumber]
      );
      console.log("✅ New user saved.");
    } else {
      console.log("🔄 User already exists. Updating phone number...");
      await pool.query("UPDATE users SET phone_number = $1 WHERE id = $2", [phoneNumber, uid]);
    }

    res.json({ message: "User saved successfully", uid, username, phoneNumber });
  } catch (error) {
    console.error("❌ ERROR:", error);
    res.status(500).json({ error: "Internal Server Error", details: error.message });
  }
});

// 🚀 Upload Profile Picture (Auto-Update User Profile)
app.post("/upload-profile", upload.single("profile"), async (req, res) => {
  try {
    console.log("📩 Upload request received");

    const { uid } = req.body;
    const file = req.file;
    if (!uid || !file) return res.status(400).json({ error: "UID and file required" });

    // Upload to S3
    const fileKey = `profile_pics/${uid}-${Date.now()}.jpg`;
    const params = {
      Bucket: process.env.AWS_BUCKET_NAME,
      Key: fileKey,
      Body: file.buffer,
      ContentType: file.mimetype,
    };
    await s3.send(new PutObjectCommand(params));
    
    // Construct new Image URL with timestamp
    const imageUrl = `https://${process.env.AWS_BUCKET_NAME}.s3.${process.env.AWS_REGION}.amazonaws.com/${fileKey}`;

    // Update DB
    await pool.query("UPDATE users SET profile_pic = $1 WHERE id = $2", [imageUrl, uid]);

    // Update Redis Cache
    let cachedUser = await redis.get(`user:${uid}`);
    if (cachedUser) {
      let userData = JSON.parse(cachedUser);
      userData.profile_pic = imageUrl;
      await redis.set(`user:${uid}`, JSON.stringify(userData), "EX", 86400);
    }

    res.json({ message: "Profile updated", imageUrl });
  } catch (error) {
    console.error("❌ Error:", error);
    res.status(500).json({ error: "Internal Server Error" });
  }
});


// 🚀 Fetch User by UID
app.get("/get-user", async (req, res) => {
  try {
    console.log("\n🔍 Checking cache for UID:", req.query.uid);
    
    const { uid } = req.query;
    if (!uid) return res.status(400).json({ error: "User UID is required" });

    // Check Redis Cache
    const cachedUser = await redis.get(`user:${uid}`);
    if (cachedUser) {
      console.log("✅ Cache hit! Returning cached user data.");
      return res.json(JSON.parse(cachedUser));
    }

    // Fetch from DB if not in cache
    console.log("🚀 Cache miss! Fetching from DB...");
    const user = await pool.query(
      "SELECT id, username, profile_pic, phone_number FROM users WHERE id = $1",
      [uid]
    );

    if (user.rows.length === 0) return res.status(404).json({ error: "User not found" });

    const userData = user.rows[0];

    // Store in Redis with expiration (1 day)
    await redis.set(`user:${uid}`, JSON.stringify(userData), "EX", 86400);

    res.json(userData);
  } catch (error) {
    console.error("❌ Error fetching user:", error);
    res.status(500).json({ error: "Internal Server Error" });
  }
});


// 🚀 Start Server
const PORT = process.env.PORT || 4000;
app.listen(PORT, () => console.log(`🚀 Server running on port ${PORT}`));
