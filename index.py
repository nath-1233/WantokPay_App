import sys
from pathlib import Path

backend_directory = str(Path(__file__).parent / "backend")
if backend_directory not in sys.path:
    sys.path.insert(0, backend_directory)

from app.main import app
