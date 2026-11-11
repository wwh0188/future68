#!/usr/bin/env python3
"""Offline representative controls from the unmodified 1.2.6 APK; never opens BLE.

Usage: generate_custom_control_oracle.py hoyi-1.2.6.apk output.tsv
Frames prove encoding agreement, not firmware or physical-machine acceptance.
"""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import zipfile

from generate_factory_wire_oracle import function

APK_SHA256 = "26133c2ac8ecf3d5ee7682bafa6555b1c78836749c937477b05214a88d8ea238"
BUNDLE_SHA256 = "4ed90ea99240b7f37373cdb5c9df0bf315103ca7aea6aaab6f37b2c58dd564d7"
# Original function bodies are extracted verbatim; only the harness supplies offline globals.
ENCODER_SHA256 = "06ac7d037cb241abec5c16a1825645792c1942d8356dadd2b35e9b99fef85a31"


def harness(source: str) -> str:
    start = source.index("addd:function(t,e,i){")
    module = source[start:source.index("setPress:function", start) + 1000]
    helpers = "\n".join(function(module, f"function {name}(", True)
                        for name in ("g", "y", "w", "b", "_", "x"))
    methods = {name: function(source[start:], f"{name}:function", False)
               for name in ("startTempChart", "startChart", "setPress", "ten2Hex", "xor_fun")}
    return "(function(){\n" + helpers + "\nreturn {" + ",".join(
        f"{name}:{body}" for name, body in methods.items()) + "};})()"


# PRESSURE/follow; FLOW/Ls and Hs; FLOW with independent first-segment flow; wire edges.
CASES = [
    ("pressure-follow", [1, 0, 2, 7, 92, 55, 0, 8, 30, 90, 0, 0, 150, 12, 400, 0, 0]),
    ("flow-ls", [0, 1, 3, 7, 92, 70, 0, 0, 20, 38, 20, 0, 160, 5, 400, 140, 0]),
    ("flow-hs", [1, 1, 3, 7, 92, 70, 0, 8, 20, 38, 20, 0, 160, 12, 400, 140, 0]),
    ("first-flow", [0, 1, 2, 1, 92, 55, 1, 8, 20, 38, 0, 0, 150, 12, 400, 0, 0]),
    ("pressure-time-edges", [0, 0, 1, 5, 105, 1, 0, 127, 120, 0, 0, 0, 10, 255, 0, 0, 0]),
]
NODE = r'''
const vm=require('node:vm'); let input='';
process.stdin.on('data',part=>input+=part).on('end',()=>{
 const {harness,cases}=JSON.parse(input),data={scaleSta:false};
 const legacy=vm.runInNewContext(harness,{getApp:()=>({globalData:data}),e:()=>{}},{timeout:1000});
 const rows=[]; let writes=[];
 legacy.BleWrite=(hex,size)=>writes.push({hex:hex.toUpperCase(),size});
 function take(size){if(writes.length!==1||writes[0].size!==size)throw Error('unexpected writes');
   const hex=writes[0].hex;writes=[];return hex;}
 for(const [p,f] of [[0,0],[1,0],[1,1],[0,1]]){
   legacy.setPress(!!p,!!f);rows.push(['lever',`${p},${f}`,take(5)]);
 }
 for(const [name,p] of cases){
   const row={press:!!p[0],chart:!!p[1],seg:p[2],temp:p[4],flow:p[5],seg1FlowMode:!!p[6],
     time:p[7],press1:p[8],press2:p[9],press3:p[10],press4:p[11],flow1:p[12]/10,
     time1:p[13],flow2:p[14]/10,flow3:p[15]/10,flow4:p[16]/10};
   if(p[3]===7)legacy.startTempChart(7,row,1);
   else {data.chartList=Array(5).fill(null);data.chartList[p[3]-1]=row;legacy.startChart(p[3],1);}
   rows.push([name,p.join(','),take(20)]);
 }
 process.stdout.write(JSON.stringify(rows));
});
'''


def generate(apk: Path, output: Path) -> None:
    if hashlib.sha256(apk.read_bytes()).hexdigest() != APK_SHA256:
        raise ValueError("APK differs from the reviewed original 1.2.6 artifact")
    with zipfile.ZipFile(apk) as archive:
        bundle = archive.read("assets/apps/__UNI__7D80DAB/www/app-service.js")
    if hashlib.sha256(bundle).hexdigest() != BUNDLE_SHA256:
        raise ValueError("original APK bundle differs from the reviewed source")
    extracted = harness(bundle.decode("utf-8"))
    if hashlib.sha256(extracted.encode()).hexdigest() != ENCODER_SHA256:
        raise ValueError("original encoder differs from the reviewed extraction")
    result = subprocess.run(["node", "-e", NODE], input=json.dumps({"harness": extracted, "cases": CASES}),
                            text=True, capture_output=True, check=True, timeout=20)
    rows = json.loads(result.stdout)
    if len(rows) != len(CASES) + 4:
        raise ValueError("incomplete original APK evidence")
    for index, (name, parameters, frame) in enumerate(rows):
        if not isinstance(frame, str) or len(frame) != (10 if index < 4 else 40) or any(
                c not in "0123456789ABCDEF" for c in frame):
            raise ValueError("invalid original APK frame")
    lines = [f"# custom-control-wire-v1\tapk-sha256={APK_SHA256}\tsource-sha256={BUNDLE_SHA256}"
             f"\tencoder-sha256={ENCODER_SHA256}"]
    lines += ["\t".join(row) for row in rows]
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    generate(*map(Path, sys.argv[1:]))
    print("generated 4 lever and 5 custom curve representative frames; offline only")
