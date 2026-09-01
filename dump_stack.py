import sys
text = sys.stdin.read()
stack = []
for i, c in enumerate(text):
    if c == '{': stack.append(text.count('\n', 0, i) + 1)
    elif c == '}': 
        if stack: stack.pop()
print("Stack:", stack)
