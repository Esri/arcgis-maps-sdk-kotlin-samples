# README and Metadata Style Checks

This folder contains the Docker-based checker used by GitHub Actions to validate sample documentation in this repository.

The action is wired through [action.yml](action.yml) and runs automatically in CI for changed sample documentation files. The container entrypoint executes:

* `mdl` for Markdown linting of sample `README.md` files
* `README_style_checker.py` for sample README structure and title rules
* `metadata_style_checker.py` for sample `README.metadata.json` validation

The current action scope is limited to files under `samples/` named `README.md` or `README.metadata.json`.

Use this README to run the same checks locally before pushing changes.

## Prerequisites

For local usage:

* Docker Desktop or another working Docker runtime
* Git
* Python 3 available on the host shell for building the JSON file list passed into Docker
* Run commands from anywhere inside this repository
* For diff-based commands, make sure the comparison branch exists locally, for example:

```bash
git fetch origin v.next
```

Build the checker image before running the examples below:

```bash
docker build -t readme-metadata-stylecheck \
  -f tools/CI/README_Metadata_StyleCheck/Dockerfile.dockerfile \
  tools/CI/README_Metadata_StyleCheck
```

Rebuild the image any time files are changed in this folder, such as `entry.py`, `README_style_checker.py`, `metadata_style_checker.py`, or `style.rb`.

## Example 1: Check the Current Sample Folder

Use this when already inside a specific sample folder such as `samples/show-line-of-sight-analysis-in-map/`.

This command detects the current sample directory, collects `README.md` and `README.metadata.json` if present, and runs both checkers in Docker using the repository root as the mounted workspace.

```bash
repo_root=$(git rev-parse --show-toplevel) && \
sample_dir=$(python3 -c 'import os,sys; print(os.path.relpath(os.getcwd(), sys.argv[1]))' "$repo_root") && \
file_list=$(python3 -c 'import json,os,sys; sample_dir,repo_root=sys.argv[1:3]; candidates=[f"{sample_dir}/README.md", f"{sample_dir}/README.metadata.json"]; print(json.dumps([path for path in candidates if os.path.exists(os.path.join(repo_root, path))]))' "$sample_dir" "$repo_root") && \
docker run --rm -v "$repo_root":/github/workspace -w /github/workspace \
  readme-metadata-stylecheck \
  --string "$file_list"
```

Expected result: the command exits with code `0` when both the README and metadata checks pass for the current sample.

## Example 2: Check Current Branch Diffs and Local Doc Edits

Use this to simulate what CI would check for sample docs changed against `origin/v.next`, while also including local staged and unstaged README or metadata edits.

```bash
repo_root=$(git rev-parse --show-toplevel) && \
cd "$repo_root" && \
changed_docs=$( \
  { \
    git diff --name-only origin/v.next...HEAD -- \
      'samples/**/README.md' \
      'samples/**/README.metadata.json'; \
    git diff --name-only --cached -- \
      'samples/**/README.md' \
      'samples/**/README.metadata.json'; \
    git diff --name-only -- \
      'samples/**/README.md' \
      'samples/**/README.metadata.json'; \
  } | awk 'NF' | sort -u \
    | python3 -c 'import json,sys; print(json.dumps([line.strip() for line in sys.stdin if line.strip()]))' \
) && \
echo "$changed_docs" && \
docker run --rm -v "$repo_root":/github/workspace -w /github/workspace \
  readme-metadata-stylecheck \
  --string "$changed_docs"
```

Expected result: the command prints the exact sample doc files that will be checked, then exits with code `0` if they all pass.

## Example 3: Check All Samples

Use this for a full repository sweep of every sample README and metadata file, regardless of current git diffs.

```bash
repo_root=$(git rev-parse --show-toplevel) && \
cd "$repo_root" && \
all_sample_docs=$( \
  find samples -type f \( -name 'README.md' -o -name 'README.metadata.json' \) \
  | LC_ALL=C sort \
  | python3 -c 'import json,sys; print(json.dumps([line.strip() for line in sys.stdin if line.strip()]))' \
) && \
echo "$all_sample_docs" && \
docker run --rm -v "$repo_root":/github/workspace -w /github/workspace \
  readme-metadata-stylecheck \
  --string "$all_sample_docs"
```

Expected result: the command performs a full documentation sweep across all samples and exits with code `0` only if all README and metadata files pass.

## Notes

* Running `entry.py` directly on the host is not the intended local workflow. It expects container paths such as `/README_style_checker.py` and `/style.rb`.
* The Docker workflow above is the closest local match to GitHub Actions behavior.
* The diff-based command is intentionally scoped to sample `README.md` and `README.metadata.json` files because that is the only content this action evaluates.
