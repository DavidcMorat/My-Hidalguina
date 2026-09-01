import sys
text = sys.stdin.read()
stack = []
for i, c in enumerate(text):
    line = text.count('\n', 0, i) + 1
    if c == '{': 
        stack.append(line)
    elif c == '}': 
        if stack:
            stack.pop()
        else:
            print("Extra } at line", line)
            
print("Unclosed { at:", stack)
