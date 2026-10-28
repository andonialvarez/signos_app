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
# Each result: <h2 class="title">lemma</h2> … stories/<name>.png … <p class="parrafo_descripcion">definition</p>
ENTRY_RE = re.compile(
    r"""<h2 class="title">(.*?)</h2>.*?bddilse/images/stories/([^"'<>?#]+?)\.(?:png|jpe?g)["'].*?parrafo_descripcion">(.*?)</p>""",
    re.I | re.S,
)
NOT_FOUND = "No se ha encontrado ningún resultado"
# Suffixes DILSE uses for variants of the same sign: one or two hands, letters, numbers.
VARIANT = {"mon", "bim", "ma", "mp"}


def norm(s):
    s = unicodedata.normalize("NFD", html.unescape(s).lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", " ", s).strip()


def strip_tags(s, sep=" "):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", sep, s))).strip()


def unmangle(s):
    """DILSE stores some text encoded twice ("dÃ­a"); undo it when possible."""
    try:
        return s.encode("latin-1").decode("utf-8")
    except (UnicodeEncodeError, UnicodeDecodeError):
        return s


def split_lemma(lemma):
    """"tarde (buenas tardes)" -> ("tarde", "buenas tardes"); "bueno, na" -> ("bueno", "")."""
    m = re.match(r"\s*([^(]*?)\s*(?:\((.*)\))?\s*$", lemma)
    main, sub = (m.group(1), m.group(2) or "") if m else (lemma, "")
    return main.split(",")[0].strip(), sub.strip()


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
    """Media found on a result page, in order, with the lemma and definition of each entry."""
    info = {}
    for m in ENTRY_RE.finditer(page):
        name = urllib.parse.unquote(m.group(2))
        info.setdefault(name, (unmangle(strip_tags(m.group(1), "")), unmangle(strip_tags(m.group(3)))))
    found = {}
    for m in MEDIA_RE.finditer(page):
        name = urllib.parse.unquote(m.group(1))
        ext = m.group(2).lower()
        lemma, definition = info.get(name, ("", ""))
        entry = found.setdefault(name, {"name": name, "video": None, "photo": None, "lemma": lemma, "context": definition})
        url = urllib.parse.urljoin(BASE, m.group(0))
        if ext in ("mov", "mp4", "m4v"):
            entry["video"] = entry["video"] or url
        else:
            entry["photo"] = entry["photo"] or url
    return [c for c in found.values() if c["video"]]


def suggest(term):
    """DILSE's autocomplete, which knows entries that the search page sometimes misses."""
    q = urllib.parse.quote(term)
    try:
        data = json.loads(fetch(f"{BASE}php/buscador-autocompletar_nuevo.php?buscar={q}&term={q}"))
    except Exception as e:  # noqa: BLE001
        print(f"  ! autocompletar: {e}")
        return [], ""
    out = []
    for item in data if isinstance(data, list) else []:
        if isinstance(item, dict):
            text = item.get("value") or item.get("label") or item.get("entrada") or ""
            out.append((unmangle(strip_tags(str(text), "")), str(item.get("parametros") or "")))
        elif isinstance(item, str):
            out.append((unmangle(strip_tags(item, "")), ""))
    return out, json.dumps(data, ensure_ascii=False, indent=1)


def score(cand, term, sense=None):
    t = norm(term)
    tokens = t.split()
    base = norm(cand["name"].replace("_", " ").replace("-", " "))
    btokens = base.split()
    s = 0
    lemma, sublemma = split_lemma(cand["lemma"])
    if (sublemma and norm(sublemma) == t) or (not sublemma and norm(lemma) == t):
        s += 100  # the entry's headword, or its sublemma for expressions ("buenas tardes")
    if base == t:
        s += 90
    elif base.startswith(t + " ") and all(len(x) == 1 or x.isdigit() or x in VARIANT for x in btokens[len(tokens):]):
        s += 60  # variant: "manana 2", "casa a", "hermano mon"
    elif len(tokens) > 1 and sorted(btokens) == sorted(tokens):
        s += 80  # same words in dictionary order: "noches_buenas" for "buenas noches"
    if tokens and all(x in btokens for x in tokens):
        s += 30
    s -= 3 * max(0, len(btokens) - len(tokens))
    if sense and norm(sense) in norm(cand.get("context", "")):
        s += 50  # the definition mentions the meaning we want ("letra")
    return s


def pick(term_list, explicit=None, pages=None, sense=None):
    """Searches each term until one entry matches. Returns (choice, all candidates seen, term used).

    Candidates are scored against every query of the sign, so a search for "noches" can
    find "noches_buenas" for "buenas noches". Result pages are appended to `pages`.
    """
    seen = []
    best = lambda c: max(score(c, q, sense) for q in term_list)  # noqa: E731
    for term in term_list:
        page = search(term)
        if pages is not None:
            pages.append((term, page))
        cands = candidates(page)
        if not cands:
            # Some entries ("gracias") only come up through the autocomplete.
            tokens = norm(term).split()
            hints, raw = suggest(term)
            if pages is not None and raw:
                pages.append((f"autocompletar {term}", raw))
            for text, params in [h for h in hints if all(x in norm(h[0]).split() for x in tokens)][:3]:
                page = fetch(urllib.parse.urljoin(BASE, params)) if params else search(text)
                if pages is not None:
                    pages.append((text, page))
                cands += [c for c in candidates(page) if c["name"] not in {x["name"] for x in cands}]
        seen.extend({**c, "term": term} for c in cands)
        if explicit:
            for c in cands:
                if c["name"] == explicit:
                    return c, seen, term
            continue
        if sense:
            # Only entries whose definition has that meaning ("letra" leaves out the preposition "a").
            cands = [c for c in cands if norm(sense) in norm(c["context"])]
        ranked = sorted(cands, key=lambda c: -best(c))
        if ranked and best(ranked[0]) >= 30:
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
        signs[w["key"]] = {"label": w["label"], "q": w.get("q") or [w["w"]], "file": w.get("file"), "sense": w.get("sense")}
    for key, s in list(data["signs"].items()) + [(x["key"], x) for x in data.get("letters", [])]:
        signs[key] = {"label": s["label"], "q": s["q"], "file": s.get("file"), "sense": s.get("sense")}
    return signs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--content", required=True)
    ap.add_argument("--old", default="")
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--only", default="")
    ap.add_argument("--dump", default="", help="keys whose search pages are saved in the report (always for missing signs)")
    ap.add_argument("--review", action="store_true", help="also search the signs already published, to list their meanings")
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    os.makedirs(args.report, exist_ok=True)
    signs = load_signs(args.content)
    old = {}
    if args.old and os.path.exists(args.old):
        old = json.load(open(args.old, encoding="utf-8")).get("items", {})
    only = {k.strip() for k in args.only.split(",") if k.strip()}
    dump = {k.strip() for k in args.dump.split(",") if k.strip()}

    items, rows = {}, []
    for i, (key, sign) in enumerate(signs.items(), 1):
        prev = old.get(key)
        redo = args.force or key in only or key in dump or prev is None or (sign["file"] and prev.get("src") != sign["file"])
        if not redo:
            items[key] = prev
            if not args.review:
                continue
        print(f"[{i}/{len(signs)}] {key}: {', '.join(sign['q'])}", flush=True)
        pages = []
        try:
            choice, seen, term = pick(sign["q"], sign["file"], pages, sign["sense"])
        except Exception as e:  # noqa: BLE001
            choice, seen, term = None, [], None
            print(f"  ! búsqueda: {e}")
        if not redo:
            # Review only: keep the published video and note what the search would choose now.
            kept = next((c for c in seen if c["name"] == prev["src"]), {"lemma": prev.get("lemma", ""), "context": ""})
            rows.append({
                "key": key, "label": sign["label"], "queries": sign["q"], "term": term, "status": "kept",
                "chosen": prev["src"], "lemma": kept["lemma"], "context": kept["context"],
                "now": choice["name"] if choice and choice["name"] != prev["src"] else None,
                "candidates": [{"name": c["name"], "lemma": c["lemma"], "def": c["context"], "term": c["term"]} for c in seen],
            })
            continue
        if key in dump or not choice:
            os.makedirs(os.path.join(args.report, "pages"), exist_ok=True)
            for n, (q, page) in enumerate(pages, 1):
                with open(os.path.join(args.report, "pages", f"{key}.{n}.html"), "w", encoding="utf-8") as f:
                    f.write(f"<!-- buscar={q} -->\n{page}")
        row = {
            "key": key, "label": sign["label"], "queries": sign["q"], "term": term,
            "chosen": choice["name"] if choice else None,
            "lemma": choice["lemma"] if choice else "",
            "context": choice["context"] if choice else "",
            "candidates": [{"name": c["name"], "lemma": c["lemma"], "def": c["context"], "term": c["term"]} for c in seen],
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
        f"{len(items)} de {len(signs)} signos publicados ({total / 1e6:.1f} MB). Procesados ahora: {sum(r['status'] != 'kept' for r in rows)}.",
        "",
    ]
    if missing:
        lines += ["## Sin vídeo", "", ", ".join(missing), ""]
    if rows:
        cell = lambda t: t.replace("|", "/")[:90]  # noqa: E731
        lines += [
            "## Procesados y revisados", "",
            "| Clave | Signo | Elegido | Significado en DILSE | Otras acepciones |", "|---|---|---|---|---|",
        ]
        for r in rows:
            others = {}
            for c in r["candidates"]:
                if c["name"] != r["chosen"]:
                    others.setdefault(c["name"], c["def"])
            others = "; ".join(f"{n}: {cell(d)[:40]}" for n, d in others.items())[:400]
            chosen = r["chosen"] or "❌"
            if r.get("now"):
                chosen += f" (ahora elegiría {r['now']})"
            lines.append(f"| {r['key']} | {r['label']} | {chosen} | {cell(r['lemma'] + ': ' + r['context'])} | {cell(others)[:400]} |")
    open(os.path.join(args.report, "summary.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print("\n".join(lines[:3]))
    failed = [r for r in rows if r["status"].startswith("error")]
    if failed and len(failed) == len([r for r in rows if r["status"] != "kept"]):
        sys.exit("Todos los signos han fallado: revisa la conexión con DILSE.")


if __name__ == "__main__":
    main()
