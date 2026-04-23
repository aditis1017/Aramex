const express = require("express");
const { exec } = require("child_process");

const app = express();
app.use(express.json());

// ✅ UPDATE THIS PATH
const projectPath = "C:/Users/Aditi Singh/NEW-GIT/Corp.microservice.ccm-automation";

// 🔹 Execute Existing Test Suite (Dynamic)
app.post("/execute", (req, res) => {

  const suiteName = req.body?.suite || "TC_003-Suite";  // default

  const command = `"C:/Users/Aditi Singh/Katalon Studio/Katalon_Studio_Engine_Windows_64-11.1.1/katalonc.exe" -noSplash -runMode=console -apiKey=20ede496-f885-406f-8b46-a0c4c3fa2f0c -projectPath="C:/Users/Aditi Singh/NEW-GIT/Corp.microservice.ccm-automation/Dummy-Test.prj" -testSuitePath="Test Suites/TC_003-Suite" -browserType="Chrome"`;

  console.log("Running Command:", command);

  exec(command, (err, stdout, stderr) => {
    if (err) {
      return res.send(`❌ Error:\n${err.message}`);
    }
    res.send(`✅ Execution Result:\n${stdout}`);
  });
});

app.listen(3000, () => {
  console.log("🚀 AI Agent Ready → http://localhost:3000");
});