import sys
text = sys.stdin.read()
stack = []
for i, c in enumerate(text):
    if c == '{': stack.append(text.count('\n', 0, i) + 1)
    elif c == '}': 
        if stack: 
            opened = stack.pop()
            if opened == int(sys.argv[1]):
                print("Closed at", text.count('\n', 0, i) + 1)
