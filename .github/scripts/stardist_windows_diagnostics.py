"""Compare standalone imports and model loading with JDLL's Windows launch settings."""
import json
import os
from pathlib import Path
import subprocess
import sys

prefix = Path(sys.prefix)
models = list(Path(sys.argv[1]).glob("*/config.json"))
if len(models) != 1:
    raise RuntimeError(f"Expected one downloaded StarDist model, found {models}")
model_dir = models[0].parent
print(f"Python: {sys.executable}\nEnvironment: {prefix}\nModel: {model_dir}", flush=True)

normal = os.environ.copy()
cleared = normal.copy()
for key in list(cleared):
    if key.upper() in ("PATH", "PYTHONPATH", "PYTHONHOME"):
        del cleared[key]
cleared.update(PATH="", PYTHONHOME="", CUDA_VISIBLE_DEVICES="-1")
restored = cleared.copy()
restored["PATH"] = os.pathsep.join(str(path) for path in (
    prefix, prefix / "Library" / "bin", prefix / "Library",
    prefix / "Scripts", Path(os.environ["SystemRoot"]) / "System32"
))

imports = {
    "tensorflow": "import tensorflow as tf; print(tf.__version__, flush=True)",
    "stardist": "from stardist.models import StarDist2D; print('StarDist OK', flush=True)",
    "stardist-worker-thread": """
import numpy
from threading import Thread
def load():
    from stardist.models import StarDist2D
    print('StarDist worker thread OK', flush=True)
t = Thread(target=load)
t.start()
t.join()
""",
    "load-model": f"""
from stardist.models import StarDist2D
print('StarDist imported; loading model', flush=True)
model = StarDist2D(None, name={model_dir.name!r}, basedir={str(model_dir.parent)!r})
print('MODEL LOADED', flush=True)
""",
}

results = []
for mode, environment in (("normal", normal), ("jdll-empty-path", cleared),
                          ("environment-dll-path", restored)):
    for name, code in imports.items():
        print(f"\n=== {mode}: {name} ===", flush=True)
        try:
            result = subprocess.run(
                [sys.executable, "-u", "-X", "faulthandler", "-c", code],
                cwd=prefix, env=environment, timeout=90,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                errors="replace",
            )
            print(result.stdout, flush=True)
            success = result.returncode == 0 and (
                name != "load-model" or "MODEL LOADED" in result.stdout
            ) and (name != "stardist-worker-thread" or "StarDist worker thread OK" in result.stdout)
            exit_code = f"{result.returncode} (0x{result.returncode & 0xffffffff:08X})"
        except subprocess.TimeoutExpired:
            success, exit_code = False, "TIMEOUT"
        print(f"RESULT: {exit_code}; success={success}", flush=True)
        results.append(dict(mode=mode, check=name, success=success, exit_code=exit_code))
print("\nRESULTS\n" + json.dumps(results, indent=2), flush=True)
