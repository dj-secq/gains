with open("app/src/main/java/com/example/repsgrams/data/HealthConnectManager.kt", "r") as f:
    text = f.read()
import re
text = text.replace("HealthConnectClient.isProviderAvailable(context)", "HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE")
with open("app/src/main/java/com/example/repsgrams/data/HealthConnectManager.kt", "w") as f:
    f.write(text)
