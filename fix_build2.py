with open("app/build.gradle.kts", "r") as f:
    text = f.read()

# Remove robolectric (it doesn't play nice with MigrationTestHelper)
text = text.replace(
    '    testImplementation(libs.androidx.room.testing)\n    testImplementation("org.robolectric:robolectric:4.11.1")\n    testImplementation("androidx.test.ext:junit:1.1.5")\n    testImplementation("androidx.test:core:1.5.0")',
    '    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
