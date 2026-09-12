import re
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

text = re.sub(r'item \{ SupplementAdherenceSection\([\s\S]*?\}', '', text)
text = re.sub(r'item \{ SupplySection\([\s\S]*?\}', '', text)
text = re.sub(r'@Composable\nprivate fun SupplementAdherenceSection\([\s\S]*?@Composable\nprivate fun ProgressMetric', '@Composable\nprivate fun ProgressMetric', text)
text = re.sub(r'@Composable\nprivate fun SupplyItem\([\s\S]*?@Composable\nprivate fun ProgressMetric', '@Composable\nprivate fun ProgressMetric', text)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)
