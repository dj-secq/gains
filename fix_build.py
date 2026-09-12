with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace("testImplementation(libs.junit)", "testImplementation(libs.junit)\n    testImplementation(libs.androidx.room.testing)\n    testImplementation(\"org.robolectric:robolectric:4.11.1\")\n    testImplementation(\"androidx.test.ext:junit:1.1.5\")\n    testImplementation(\"androidx.test:core:1.5.0\")")

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
