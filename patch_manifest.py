import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    text = f.read()

permissions = """    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
"""
text = text.replace('    <uses-permission android:name="android.permission.VIBRATE" />\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n', permissions)

service = """        <service
            android:name=".service.RestTimerService"
            android:foregroundServiceType="health"
            android:exported="false" />
    </application>"""
text = text.replace("    </application>", service)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(text)
