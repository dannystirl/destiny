::python3 src/main/python/armorExaminer.py
pyinstaller --onefile --console --workpath bin --distpath output --specpath bin src/main/python/armorExaminer.py
start output/armorExaminer.exe