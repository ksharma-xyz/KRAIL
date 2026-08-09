#!/usr/bin/env python3
"""Validate source captures, store panels, report copy and QA scores."""

from __future__ import annotations

import hashlib
import html
import json
import pathlib
import re
import struct
import sys


PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def png_info(path: pathlib.Path) -> tuple[int, int, bool]:
    data = path.read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG")

    width = height = color_type = None
    has_transparency_chunk = False
    offset = len(PNG_SIGNATURE)
    while offset + 12 <= len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        chunk_type = data[offset + 4:offset + 8]
        chunk = data[offset + 8:offset + 8 + length]
        if chunk_type == b"IHDR":
            width, height, _, color_type, _, _, _ = struct.unpack(">IIBBBBB", chunk)
        elif chunk_type == b"tRNS":
            has_transparency_chunk = True
        elif chunk_type == b"IEND":
            break
        offset += 12 + length

    if width is None or height is None or color_type is None:
        raise ValueError("missing PNG IHDR")
    return width, height, color_type in (4, 6) or has_transparency_chunk


def plain(fragment: str) -> str:
    return " ".join(html.unescape(re.sub(r"<[^>]+>", " ", fragment)).split())


def words(value: str) -> int:
    return len(re.findall(r"[A-Za-z0-9]+(?:['&+-][A-Za-z0-9]+)*", value))


def section(report: str, section_id: str) -> str | None:
    match = re.search(
        rf'<section class="platform" id="{re.escape(section_id)}">(.*?)</section>',
        report,
        re.S,
    )
    return match.group(1) if match else None


def score_audit(report: str, section_id: str) -> str | None:
    match = re.search(
        rf'<article class="score-audit" id="score-{re.escape(section_id)}"(.*?)</article>',
        report,
        re.S,
    )
    return match.group(1) if match else None


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: verify-listing.py <app/listing-qa.json>", file=sys.stderr)
        return 2

    config_path = pathlib.Path(sys.argv[1]).resolve()
    root = config_path.parent
    config = json.loads(config_path.read_text())
    report_path = root / config["report"]
    report = report_path.read_text()
    report_digest = hashlib.sha256(report_path.read_bytes()).hexdigest()
    copy_rules = config["copyRules"]
    max_bytes = config.get("maxOutputBytes", 8 * 1024 * 1024)
    output_index_path = root / config.get("outputIndex", "store-images/index.json")
    output_index = json.loads(output_index_path.read_text()) if output_index_path.exists() else {}
    failures: list[str] = []

    def check(ok: bool, scope: str, message: str) -> None:
        if not ok:
            failures.append(f"{scope}: {message}")

    scoring = config.get("scoring", {})
    baseline = scoring.get("baseline")
    check(baseline == 100, "Scoring", f"baseline must be 100, found {baseline!r}")
    check(bool(scoring.get("method")), "Scoring", "method is missing")

    policy_sources = {source["id"]: source for source in config.get("policySources", [])}
    check(bool(policy_sources), "Policy evidence", "no policy sources are configured")
    quoted_words_by_url: dict[str, int] = {}
    for source_id, source in policy_sources.items():
        source_scope = f"Policy evidence {source_id}"
        for field in ("store", "title", "url", "quote", "checked"):
            check(bool(source.get(field)), source_scope, f"{field} is missing")
        check(source.get("url", "").startswith("https://"), source_scope,
              "source URL must use HTTPS")
        check(words(source.get("quote", "")) <= 15, source_scope,
              "policy quote exceeds the 15-word report limit")
        check(source.get("quote", "") in plain(report), source_scope,
              "configured policy quote is missing from the report")
        check(source.get("url", "") in report, source_scope,
              "configured policy link is missing from the report")
        url = source.get("url", "")
        quoted_words_by_url[url] = quoted_words_by_url.get(url, 0) + words(source.get("quote", ""))
    for url, quoted_word_count in quoted_words_by_url.items():
        check(quoted_word_count <= 25, "Policy evidence",
              f"quotes from {url} total {quoted_word_count} words; maximum is 25")

    for gate in config.get("submissionGates", []):
        gate_scope = f"Submission gate {gate.get('title', 'untitled')}"
        check(gate.get("severity") in {"warning", "blocker"}, gate_scope,
              "severity must be warning or blocker")
        for field in ("title", "trigger", "outcome", "action", "sourceIds"):
            check(bool(gate.get(field)), gate_scope, f"{field} is missing")
        for source_id in gate.get("sourceIds", []):
            check(source_id in policy_sources, gate_scope,
                  f"unknown policy source {source_id!r}")
        check(not (gate.get("triggered") and gate.get("severity") == "blocker"),
              gate_scope, "active blocker prevents store upload")

    for platform in config["platforms"]:
        scope = platform["displayName"]
        body = section(report, platform["sectionId"])
        check(body is not None, scope, "report section is missing")
        if body is None:
            continue

        heading_match = re.search(r"<h2>(.*?)</h2>", body, re.S)
        heading = plain(heading_match.group(1)) if heading_match else ""
        check(heading == platform["displayName"], scope,
              f"prominent heading must be '{platform['displayName']}', found '{heading}'")

        articles = re.findall(r'(<article class="store-panel.*?</article>)', body, re.S)
        panels = platform["panels"]
        check(len(articles) == len(panels), scope,
              f"report has {len(articles)} panels, config has {len(panels)}")

        deductions = platform.get("deductions", [])
        score = baseline - sum(item["points"] for item in deductions)
        score_match = re.search(r'class="platform-score" data-score="(\d+)"', body)
        report_score = int(score_match.group(1)) if score_match else None
        check(report_score == score, scope,
              f"report score is {report_score}; configured score is {score}")

        explanation_match = re.search(
            rf'data-score-explanation="{re.escape(platform["sectionId"])}"(.*?)</div>',
            body,
            re.S,
        )
        explanation = explanation_match.group(1) if explanation_match else ""
        check(bool(explanation_match), scope, "immediate score explanation is missing")
        check(f"Why {score}:" in plain(explanation), scope,
              f"immediate explanation must begin with 'Why {score}:'")
        for deduction in deductions:
            check(str(deduction["points"]) in plain(explanation), scope,
                  f"immediate explanation omits -{deduction['points']} deduction")

        audit = score_audit(report, platform["sectionId"])
        check(audit is not None, scope, "detailed score audit is missing")
        audit = audit or ""
        audit_score_match = re.search(r'data-score="(\d+)"', audit)
        audit_score = int(audit_score_match.group(1)) if audit_score_match else None
        check(audit_score == score, scope,
              f"detailed audit score is {audit_score}; configured score is {score}")
        for heading in ("What is correct", "Points deducted and improvements", "Store review flag"):
            check(heading in plain(audit), scope,
                  f"detailed audit is missing '{heading}'")

        strengths = platform.get("strengths", [])
        check(len(strengths) >= 3, scope, "at least three strengths are required")
        for strength in strengths:
            check(strength in plain(audit), scope,
                  f"configured strength is missing from report: {strength!r}")

        for deduction in deductions:
            deduction_scope = f"{scope} -{deduction.get('points', '?')} deduction"
            check(isinstance(deduction.get("points"), int) and deduction["points"] > 0,
                  deduction_scope, "points must be a positive integer")
            for field in ("category", "reason", "impact", "improvement", "storeRisk"):
                check(bool(deduction.get(field)), deduction_scope, f"{field} is missing")
            check(deduction.get("category", "") in plain(audit), deduction_scope,
                  "category is missing from detailed audit")
            check(f"-{deduction['points']}" in plain(audit), deduction_scope,
                  "point deduction is missing from detailed audit")

        policy_review = platform.get("policyReview", {})
        policy_status = policy_review.get("status")
        check(policy_status in {"pass", "warning", "blocker"}, scope,
              f"invalid policy status {policy_status!r}")
        for field in ("title", "issue", "reviewOutcome", "remediation", "sourceIds"):
            check(bool(policy_review.get(field)), scope,
                  f"policy review {field} is missing")
        check(f'data-policy-status="{policy_status}"' in audit, scope,
              "detailed audit policy status does not match config")
        check(policy_review.get("title", "") in plain(audit), scope,
              "policy review title is missing from detailed audit")
        for source_id in policy_review.get("sourceIds", []):
            check(source_id in policy_sources, scope,
                  f"policy review references unknown source {source_id!r}")
        check(policy_status != "blocker", scope,
              f"active policy blocker: {policy_review.get('title', 'unknown')}")

        source_hashes: set[str] = set()
        output_dir = root / platform["outputDir"]
        output_meta = output_index.get(platform.get("outputKey", platform["id"]), {})
        check(output_meta.get("reportSha256") == report_digest, scope,
              "rendered outputs are stale relative to the report")
        expected_outputs = {panel["output"] for panel in panels}
        actual_outputs = {path.name for path in output_dir.glob("*.png")}
        check(actual_outputs == expected_outputs, scope,
              "rendered output filenames do not match the configured panel set")

        for index, panel in enumerate(panels):
            panel_scope = f"{scope} panel {index + 1:02d}"
            source_path = root / panel["source"]
            output_path = output_dir / panel["output"]
            check(source_path.exists(), panel_scope, f"missing source {panel['source']}")
            check(output_path.exists(), panel_scope, f"missing output {panel['output']}")

            article = articles[index] if index < len(articles) else ""
            source_match = re.search(r'<img[^>]+src="([^"]+)"', article)
            report_source = source_match.group(1) if source_match else None
            check(report_source == panel["source"], panel_scope,
                  f"report source is {report_source!r}, expected {panel['source']!r}")

            copy_fragments: list[tuple[str, str, int]] = []
            for tag, label, limit in (
                ("strong", "headline", copy_rules["headlineMaxWords"]),
                ("small", "subline", copy_rules["sublineMaxWords"]),
            ):
                match = re.search(rf"<{tag}[^>]*>(.*?)</{tag}>", article, re.S)
                if match:
                    copy_fragments.append((label, plain(match.group(1)), limit))
            for class_name in ("sticker", "ghost"):
                match = re.search(
                    rf'<span class="[^"]*\b{class_name}\b[^"]*"[^>]*>(.*?)</span>',
                    article,
                    re.S,
                )
                if match:
                    copy_fragments.append((class_name, plain(match.group(1)), 4))

            for label, value, limit in copy_fragments:
                if copy_rules.get("forbidFullStops"):
                    check("." not in value, panel_scope,
                          f"{label} contains a forbidden full stop: {value!r}")
                check(words(value) <= limit, panel_scope,
                      f"{label} has {words(value)} words; maximum is {limit}: {value!r}")

            if source_path.exists():
                try:
                    width, height, _ = png_info(source_path)
                    check([width, height] == platform["sourceDimensions"], panel_scope,
                          f"source is {width}x{height}; expected {platform['sourceDimensions'][0]}x{platform['sourceDimensions'][1]}")
                    digest = hashlib.sha256(source_path.read_bytes()).hexdigest()
                    check(digest not in source_hashes, panel_scope,
                          "source capture duplicates another panel in this platform")
                    source_hashes.add(digest)
                    rendered_input = output_meta.get("inputs", {}).get(panel["output"], {})
                    check(rendered_input.get("source") == panel["source"], panel_scope,
                          "render index points to a different source capture")
                    check(rendered_input.get("sourceSha256") == digest, panel_scope,
                          "rendered output is stale relative to its source capture")
                except ValueError as error:
                    check(False, panel_scope, f"invalid source PNG: {error}")

            if output_path.exists():
                try:
                    width, height, has_alpha = png_info(output_path)
                    check([width, height] == platform["outputDimensions"], panel_scope,
                          f"output is {width}x{height}; expected {platform['outputDimensions'][0]}x{platform['outputDimensions'][1]}")
                    check(not has_alpha, panel_scope, "output PNG has transparency")
                    check(output_path.stat().st_size <= max_bytes, panel_scope,
                          f"output exceeds {max_bytes} bytes")
                except ValueError as error:
                    check(False, panel_scope, f"invalid output PNG: {error}")

        upload_dir = root / platform["uploadDir"]
        upload_count = platform["uploadCount"]
        expected_uploads = {panel["output"] for panel in panels[:upload_count]}
        actual_uploads = {path.name for path in upload_dir.glob("*.png")}
        check(actual_uploads == expected_uploads, scope,
              "primary upload folder does not match the approved panel subset")
        for optional in platform.get("optionalUploads", []):
            check((root / optional["dir"] / optional["file"]).exists(), scope,
                  f"missing optional upload {optional['dir']}/{optional['file']}")

        deduction_text = "; ".join(
            f"-{item['points']} {item['reason']}" for item in deductions
        ) or "no deductions"
        print(f"PASS {scope}: {score}/100; policy={policy_status} ({deduction_text})")

    if failures:
        print("\nFAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("\nAll configured store-listing checks passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
