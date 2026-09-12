with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "UserStats(bw, streak)" in line:
        new_lines.append(line)
        new_lines.append("    }\n")
        skip = True
    elif skip and "val uiState: StateFlow<ProgressUiState> = combine(" in line:
        skip = False
        new_lines.append(line)
    elif not skip:
        # replace trailing comma
        if i > 0 and "UserStats(bw, streak)" in lines[i-1]:
            pass
        else:
            new_lines.append(line)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.writelines(new_lines)
