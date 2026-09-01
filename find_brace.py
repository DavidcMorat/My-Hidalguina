import sys
text = sys.stdin.read()
stack = []
for i, c in enumerate(text):
    if c == '{': stack.append(i)
    elif c == '}': 
        if stack: stack.pop()
        else: print("Extra } at", i)
print("Unclosed { at:")
for pos in stack:
    line = text.count('\n', 0, pos) + 1
    print("Line", line)
