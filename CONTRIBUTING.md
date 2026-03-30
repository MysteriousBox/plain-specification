Javadoc Author Requirement

All Java source files in this repository must include a Javadoc comment at the top-level type (class/interface/annotation) that contains an @author tag.

- Author to use: Jayden.Liang
- Purpose: Keep consistent attribution across files and ensure documentation tools capture the author.

How to check locally:

- There's a helper script at scripts/check-javadoc-author.ps1 that scans the repo for Java files missing the author tag.

CI:

- Optionally add a step that runs the script and fails the build if any files are missing the author tag.

