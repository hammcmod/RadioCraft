<#
.SYNOPSIS
Build, tag and publish a mod version to Modrinth (and GitHub).

.DESCRIPTION
One command for a release. By default it releases RadioCraft from this repository:

    tools\push-versions.ps1 -Version 1.9.1                   # Modrinth only, alpha, notes from doc/ReleaseNotes-1.9.md
    tools\push-versions.ps1 -Version 1.9.1 -GitHub           # also a GitHub release with the jar attached
    tools\push-versions.ps1 -Version 1.9.1 -Tag -Build       # tag the current commit 1.9.1, push it, build, publish
    tools\push-versions.ps1 -Project anotherweathermod -Version 1.9.0 -GitHub   # AWM (GitHub only, not on Modrinth)
    tools\push-versions.ps1 -Version 1.9.1 -DryRun           # show what would be sent

Tokens come from the environment (MODRINTH_TOKEN, GITHUB_TOKEN) or from ~/.radiocraft/modrinth_token and
~/.radiocraft/github_token; -ModrinthToken / -GitHubToken set them for this run. Never commit a token.

Projects, their Modrinth ids and dependency lists live in tools/modrinth_projects.json.

.PARAMETER Version
The version number, e.g. 1.9.1. Defaults to the git tag on HEAD (`git describe --tags --exact-match`).
.PARAMETER Type
alpha (default), beta or release. Anything but release is marked a pre-release on GitHub.
.PARAMETER Notes
Markdown changelog. Defaults to the project's "notes" pattern in modrinth_projects.json (for RadioCraft,
doc/ReleaseNotes-<major.minor>.md); the "# " section naming the version is used.
.PARAMETER Jar
The jar to upload. Defaults to build/libs/<jar_prefix><Version>.jar.
.PARAMETER Build
Run `gradlew build` first (unit tests included).
.PARAMETER Tag
Create the git tag <Version> on HEAD (refusing if the tree is dirty) and push it with the branch.
.PARAMETER GitHub
Also create a GitHub release for the tag, with the jar attached.
.PARAMETER NoModrinth
Skip Modrinth (GitHub only).
.PARAMETER Featured
Mark the Modrinth version as featured.
#>
[CmdletBinding()]
param(
    [string]$Project = "radiocraft",
    [string]$Version,
    [ValidateSet("alpha", "beta", "release")][string]$Type = "alpha",
    [string]$Name,
    [string]$Notes,
    [string]$Jar,
    [switch]$Build,
    [switch]$Tag,
    [switch]$GitHub,
    [switch]$NoModrinth,
    [switch]$Featured,
    [switch]$DryRun,
    [string]$ModrinthToken,
    [string]$GitHubToken
)

$ErrorActionPreference = "Stop"
$toolsDir = $PSScriptRoot
$projects = Get-Content (Join-Path $toolsDir "modrinth_projects.json") -Raw | ConvertFrom-Json
$cfg = $projects.$Project
if (-not $cfg) { throw "Unknown project '$Project'. Known: $(($projects.PSObject.Properties.Name | Where-Object { $_ -notlike '_*' }) -join ', ')" }

$root = Resolve-Path (Join-Path (Join-Path $toolsDir "..") $cfg.root)
Push-Location $root
try {
    if ($ModrinthToken) { $env:MODRINTH_TOKEN = $ModrinthToken }
    if ($GitHubToken) { $env:GITHUB_TOKEN = $GitHubToken }

    if (-not $Version) {
        $Version = (git describe --tags --exact-match 2>$null)
        if (-not $Version) { throw "No -Version given and HEAD is not tagged. Pass -Version 1.9.1 (and -Tag to tag it)." }
        $Version = $Version.TrimStart("v")
    }

    if ($Tag) {
        $dirty = git status --porcelain
        if ($dirty) { throw "The working tree has uncommitted changes; commit them before tagging $Version." }
        if (git tag --list $Version) { Write-Host "Tag $Version already exists, keeping it." }
        else { git tag $Version; if ($LASTEXITCODE) { throw "git tag failed" } }
        if (-not $DryRun) {
            git push origin HEAD --tags
            if ($LASTEXITCODE) { throw "git push failed" }
        }
    }

    if ($Build) {
        Write-Host "Building $Project $Version..."
        & .\gradlew build -q
        if ($LASTEXITCODE) { throw "gradlew build failed" }
    }

    if (-not $Jar) { $Jar = Join-Path "build/libs" ("{0}{1}.jar" -f $cfg.jar_prefix, $Version) }
    if (-not (Test-Path $Jar)) { throw "No jar at $Jar. Is HEAD at the $Version tag? Build with -Build, or pass -Jar." }

    if (-not $Notes) {
        $majorMinor = ($Version -split "\.")[0..1] -join "."
        $Notes = $cfg.notes.Replace("{version}", $Version).Replace("{major_minor}", $majorMinor)
    }
    if (-not (Test-Path $Notes)) { throw "No release notes at $Notes; write them or pass -Notes." }

    $args = @("-I", (Join-Path $toolsDir "modrinth_release.py"),
              "--project", $Project, "--version", $Version, "--jar", $Jar, "--changelog", $Notes, "--type", $Type)
    if ($Name) { $args += @("--name", $Name) }
    if ($GitHub) { $args += "--github" }
    if ($NoModrinth) { $args += "--no-modrinth" }
    if ($Featured) { $args += "--featured" }
    if ($DryRun) { $args += "--dry-run" }

    & python @args
    if ($LASTEXITCODE) { throw "publishing failed" }
}
finally {
    Pop-Location
}
