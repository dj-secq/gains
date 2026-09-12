import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    text = f.read()

target = """    <application
        android:name=".RepsGramsApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.RepsGrams">"""

replacement = """    <application
        android:name=".RepsGramsApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:enableOnBackInvokedCallback="true"
        android:theme="@style/Theme.RepsGrams">"""

text = text.replace(target, replacement)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(text)

