import json
import os
import subprocess

transcript_path = "/home/dj/.gemini/antigravity/brain/565f28d8-0de9-4b81-9597-afcf5d36495c/.system_generated/logs/transcript_full.jsonl"

with open(transcript_path, "r") as f:
    lines = f.readlines()

def apply_replace(target_file, target_content, replacement_content, start_line=1, end_line=None):
    if not os.path.exists(target_file):
        return
    with open(target_file, "r") as f:
        lines = f.readlines()
    end_line = end_line or len(lines)
    chunk = "".join(lines[start_line-1:end_line])
    if target_content in chunk:
        new_chunk = chunk.replace(target_content, replacement_content)
        lines[start_line-1:end_line] = [new_chunk]
        with open(target_file, "w") as f:
            f.writelines(lines)

for line in lines:
    try:
        data = json.loads(line)
        if "tool_calls" in data:
            for call in data["tool_calls"]:
                name = call["function"]["name"]
                args_str = call["function"]["arguments"]
                if isinstance(args_str, str):
                    args = json.loads(args_str)
                else:
                    args = args_str

                if name == "default_api:write_to_file":
                    path = args.get("TargetFile")
                    content = args.get("CodeContent")
                    if path and content:
                        os.makedirs(os.path.dirname(path), exist_ok=True)
                        with open(path, "w") as out:
                            out.write(content)

                elif name == "default_api:replace_file_content":
                    path = args.get("TargetFile")
                    target = args.get("TargetContent")
                    replacement = args.get("ReplacementContent")
                    start_line = args.get("StartLine", 1)
                    end_line = args.get("EndLine", None)
                    if path and target and replacement:
                        apply_replace(path, target, replacement, start_line, end_line)

                elif name == "default_api:run_command":
                    cmd = args.get("CommandLine", "")
                    if "cat << 'EOF' >" in cmd or "cat << EOF >" in cmd:
                        # Simple extraction
                        parts = cmd.split(">")
                        if len(parts) > 1:
                            filename = parts[1].split()[0]
                            start_idx = cmd.find("EOF") + 3
                            end_idx = cmd.rfind("EOF")
                            if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
                                # if it's a python script, save and run it
                                content = cmd[start_idx:end_idx].strip()
                                content = content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                                with open(filename, "w") as out:
                                    out.write(content)
                                if filename.endswith(".py"):
                                    subprocess.run(["python3", filename])
                                elif filename.endswith(".sh"):
                                    subprocess.run(["bash", filename])
    except Exception as e:
        pass
