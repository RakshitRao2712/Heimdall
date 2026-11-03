#!/usr/bin/env python3
"""Query OSV using public Maven package names/versions only. No source or secrets are uploaded."""
import json
import re
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parents[1]
try:
    with tempfile.TemporaryDirectory(prefix="heimdall-audit-") as directory:
        output = Path(directory) / "dependencies.txt"
        subprocess.run(["./mvnw", "-q", "dependency:list", "-DincludeScope=runtime", f"-DoutputFile={output}"], cwd=root / "backend", check=True, timeout=180)
        queries = []
        for line in output.read_text().splitlines():
            match = re.match(r"\s+([^: ]+):([^: ]+):jar:([^: ]+):(compile|runtime)", line)
            if match:
                group, artifact, version, _ = match.groups()
                queries.append({"package": {"ecosystem": "Maven", "name": group + ":" + artifact}, "version": version})
        if not queries:
            raise RuntimeError("No dependencies resolved; refusing to report a clean scan")
        findings = []
        for offset in range(0, len(queries), 100):
            batch = queries[offset:offset + 100]
            request = urllib.request.Request("https://api.osv.dev/v1/querybatch", data=json.dumps({"queries": batch}).encode(), headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(request, timeout=45) as response:
                result = json.load(response)
            if len(result["results"]) != len(batch):
                raise RuntimeError("Incomplete vulnerability response")
            for package, row in zip(batch, result["results"]):
                for issue in row.get("vulns", []):
                    findings.append({"package": package["package"]["name"], "version": package["version"], "advisory": issue["id"]})
        print(json.dumps({"dependencies_checked": len(queries), "findings": findings}, indent=2))
        sys.exit(1 if findings else 0)
except Exception as error:
    print(f"Security audit could not complete: {type(error).__name__}. Check Maven/network access; this is not a clean scan.", file=sys.stderr)
    sys.exit(2)
