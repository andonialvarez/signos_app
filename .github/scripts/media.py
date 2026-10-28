#!/usr/bin/env python3
"""Builds the sign videos and photos for the app from DILSE (Fundación CNSE).

For every sign in content.json it searches DILSE, picks the entry that matches the
word, downloads its video (.mov) and photo (.png), and converts them into small files
for phones: <key>.mp4 (H.264, 480 px wide, no sound) and <key>.jpg. Signs already in
the previous manifest are skipped unless --force or --only asks for them.

A sign can name its DILSE file explicitly with "file" (for example "mañana_2") when
the automatic choice picks the wrong meaning; the report lists every candidate.

DILSE videos and photos: © Fundación CNSE, CC BY-NC-SA 3.0.
"""
import argparse
import datetime
import html
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import unicodedata
import urllib.parse
import urllib.request

BASE = "https://fundacioncnse-dilse.org/"
UA = "SignosDaily/1.0 (+https://github.com/andonialvarez/signos_app; app educativa sin ánimo de lucro)"
MEDIA_RE = re.compile(r"""(?:https?://[^"'\s>]*?/)?bddilse/images/stories/([^"'<>?#]+?)\.(mov|mp4|m4v|png|jpg|jpeg)""", re.I)
BOLD_RE = re.compile(r"<(b|strong)\b[^>]*>(.*?)</\1>", re.I | re.S)
NOT_FOUND = "No se ha encontrado ningún resultado"


def norm(s):
    s = unicodedata.normalize("NFD", html.unescape(s).lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", " ", s).strip()


def strip_tags(s):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", s))).strip()


def fetch(url, binary=False, tries=3):
    last = None
    for attempt in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            time.sleep(0.4)  # be gentle with the dictionary's server
            if binary:
                return data
            for enc in ("utf-8", "latin-1"):
                try:
                    return data.decode(enc)
                except UnicodeDecodeError:
                    continue
        except Exception as e:  # noqa: BLE001 - report and retry any network error
            last = e
            time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"{url}: {last}")


def search(term):
    """Returns the result page for a term, trying UTF-8 and Latin-1 encodings."""
    pages = []
    for enc in ("utf-8", "latin-1"):
        try:
            q = urllib.parse.quote(term, encoding=enc)
        except UnicodeEncodeError:
            continue
        page = fetch(f"{BASE}?buscar={q}")
        pages.append(page)
        if MEDIA_RE.search(page):
            return page
    return pages[0] if pages else ""


def candidates(page):
    """Media found on a result page, in order, with the bold lemma that precedes each one."""
    bolds = [(m.start(), strip_tags(m.group(2))) for m in BOLD_RE.finditer(page)]
    found = {}
    for m in MEDIA_RE.finditer(page):
        name = urllib.parse.unquote(m.group(1))
        ext = m.group(2).lower()
        entry = found.setdefault(name, {"name": name, "video": None, "photo": None, "lemma": "", "context": ""})
        url = urllib.parse.urljoin(BASE, m.group(0))
        if ext in ("mov", "mp4", "m4v"):
            entry["video"] = entry["video"] or url
        else:
            entry["photo"] = entry["photo"] or url
        if not entry["lemma"]:
            before = [b for b in bolds if b[0] < m.start()]
            if before:
                pos, lemma = before[-1]
                entry["lemma"] = lemma
                entry["context"] = strip_tags(page[pos:pos + 600])[:160]
    return [c for c in found.values() if c["video"]]


def score(cand, term):
    t = norm(term)
    tokens = t.split()
    base = norm(cand["name"].replace("_", " ").replace("-", " "))
    btokens = base.split()
    s = 0
    if norm(cand["lemma"]) == t:
        s += 100
    if base == t:
        s += 90
    elif base.startswith(t + " ") and all(len(x) <= 2 for x in btokens[len(tokens):]):
        s += 60  # numbered or lettered variant: "manana 2", "casa a"
    if tokens and all(x in btokens for x in tokens):
        s += 30
    s -= 3 * max(0, len(btokens) - len(tokens))
    return s


def pick(term_list, explicit=None):
    """Searches each term until one entry matches. Returns (choice, all candidates seen, term used)."""
    seen = []
    for term in term_list:
        page = search(term)
        cands = candidates(page)
        seen.extend({**c, "term": term} for c in cands)
        if explicit:
            for c in cands:
                if c["name"] == explicit:
                    return c, seen, term
            continue
        ranked = sorted(cands, key=lambda c: -score(c, term))
        if ranked and score(ranked[0], term) >= 30:
            return ranked[0], seen, term
    if explicit:
        # The file may exist even if the search page did not show it.
        url = urllib.parse.urljoin(BASE, "bddilse/images/stories/" + urllib.parse.quote(explicit))
        return {"name": explicit, "video": url + ".mov", "photo": url + ".png", "lemma": "", "context": ""}, seen, "file"
    return None, seen, None


def run(cmd):
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def convert(choice, key, out):
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "src" + os.path.splitext(choice["video"])[1])
        with open(src, "wb") as f:
            f.write(fetch(quote_url(choice["video"]), binary=True))
        mp4 = os.path.join(out, key + ".mp4")
        run([
            "ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", src, "-an",
            "-vf", "scale='min(480,iw)':-2:flags=lanczos,fps=25,format=yuv420p",
            "-c:v", "libx264", "-preset", "slow", "-crf", "27", "-profile:v", "main",
            "-movflags", "+faststart", mp4,
        ])
        jpg = os.path.join(out, key + ".jpg")
        photo_ok = False
        if choice.get("photo"):
            try:
                from PIL import Image

                png = os.path.join(tmp, "src.png")
                with open(png, "wb") as f:
                    f.write(fetch(quote_url(choice["photo"]), binary=True))
                img = Image.open(png).convert("RGBA")
                bg = Image.new("RGBA", img.size, (255, 255, 255, 255))
                bg.alpha_composite(img)
                img = bg.convert("RGB")
                if img.width > 480:
                    img = img.resize((480, round(img.height * 480 / img.width)), Image.LANCZOS)
                img.save(jpg, "JPEG", quality=80, optimize=True)
                photo_ok = True
            except Exception as e:  # noqa: BLE001
                print(f"  ! foto de {key}: {e}")
        if not photo_ok:
            # Middle frame of the video as the photo.
            run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-ss", "1", "-i", mp4, "-frames:v", "1", "-q:v", "4", jpg])
        return os.path.getsize(mp4), os.path.getsize(jpg)


def quote_url(url):
    parts = urllib.parse.urlsplit(url)
    return urllib.parse.urlunsplit(parts._replace(path=urllib.parse.quote(urllib.parse.unquote(parts.path))))


def load_signs(path):
    data = json.load(open(path, encoding="utf-8"))
    signs = {}
    for w in data["words"]:
        signs[w["key"]] = {"label": w["label"], "q": w.get("q") or [w["w"]], "file": w.get("file")}
    for key, s in data["signs"].items():
        signs[key] = {"label": s["label"], "q": s["q"], "file": s.get("file")}
    return signs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--content", required=True)
    ap.add_argument("--old", default="")
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--only", default="")
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    os.makedirs(args.report, exist_ok=True)
    signs = load_signs(args.content)
    old = {}
    if args.old and os.path.exists(args.old):
        old = json.load(open(args.old, encoding="utf-8")).get("items", {})
    only = {k.strip() for k in args.only.split(",") if k.strip()}

    items, rows = {}, []
    for i, (key, sign) in enumerate(signs.items(), 1):
        prev = old.get(key)
        redo = args.force or key in only or prev is None or (sign["file"] and prev.get("src") != sign["file"])
        if not redo:
            items[key] = prev
            continue
        print(f"[{i}/{len(signs)}] {key}: {', '.join(sign['q'])}", flush=True)
        try:
            choice, seen, term = pick(sign["q"], sign["file"])
        except Exception as e:  # noqa: BLE001
            choice, seen, term = None, [], None
            print(f"  ! búsqueda: {e}")
        row = {
            "key": key, "label": sign["label"], "queries": sign["q"], "term": term,
            "chosen": choice["name"] if choice else None,
            "lemma": choice["lemma"] if choice else "",
            "context": choice["context"] if choice else "",
            "candidates": [{"name": c["name"], "lemma": c["lemma"], "term": c["term"]} for c in seen],
        }
        if choice:
            try:
                mp4, jpg = convert(choice, key, args.out)
                items[key] = {"src": choice["name"], "lemma": choice["lemma"], "mp4": mp4, "jpg": jpg}
                row["status"] = "ok"
                print(f"  -> {choice['name']} ({mp4 // 1024} KB)")
            except Exception as e:  # noqa: BLE001
                row["status"] = f"error: {e}"
                print(f"  ! {e}")
                if prev:
                    items[key] = prev
        else:
            row["status"] = "missing"
            print("  ! no encontrado")
            if prev:
                items[key] = prev
        rows.append(row)

    # Keys removed from content.json disappear from the catalog.
    items = {k: v for k, v in items.items() if k in signs}
    missing = sorted(k for k in signs if k not in items)
    manifest = {
        "generated": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "source": "DILSE, Diccionario de la Lengua de Signos Española. © Fundación CNSE. https://fundacioncnse-dilse.org",
        "license": "CC BY-NC-SA 3.0 https://creativecommons.org/licenses/by-nc-sa/3.0/",
        "items": items,
        "missing": missing,
    }
    json.dump(manifest, open(os.path.join(args.out, "manifest.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(rows, open(os.path.join(args.report, "report.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    total = sum(v["mp4"] + v["jpg"] for v in items.values())
    lines = [
        "# Vídeos de los signos",
        "",
        f"{len(items)} de {len(signs)} signos publicados ({total / 1e6:.1f} MB). Procesados ahora: {len(rows)}.",
        "",
    ]
    if missing:
        lines += ["## Sin vídeo", "", ", ".join(missing), ""]
    if rows:
        lines += ["## Procesados", "", "| Clave | Buscado | Elegido | Lema | Otros candidatos |", "|---|---|---|---|---|"]
        for r in rows:
            others = ", ".join(sorted({c["name"] for c in r["candidates"]} - {r["chosen"]}))[:200]
            lines.append(f"| {r['key']} | {r['term'] or '-'} | {r['chosen'] or '❌'} | {r['lemma']} | {others} |")
    open(os.path.join(args.report, "summary.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print("\n".join(lines[:3]))
    failed = [r for r in rows if r["status"].startswith("error")]
    if failed and len(failed) == len(rows):
        sys.exit("Todos los signos han fallado: revisa la conexión con DILSE.")


if __name__ == "__main__":
    main()
