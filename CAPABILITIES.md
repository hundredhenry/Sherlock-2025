# GitHub Copilot Coding Agent Capabilities

This document describes what the GitHub Copilot Coding Agent can do when working on this repository.

## Overview

The GitHub Copilot Coding Agent is an advanced AI assistant designed to help with software development tasks. It operates within a sandboxed environment with access to the repository and various development tools.

## Core Capabilities

### 1. Code Understanding and Analysis
- **Read and analyze code** across multiple programming languages (Java, JavaScript, CSS/Sass, Gradle, etc.)
- **Navigate repository structure** to understand project organization
- **Trace dependencies** and understand how different components interact
- **Review existing tests** to understand expected behavior
- **Analyze build configurations** (Gradle, Maven, npm, etc.)

### 2. Code Modification
- **Make surgical, minimal changes** to existing code
- **Fix bugs** by identifying issues and implementing solutions
- **Refactor code** while preserving existing functionality
- **Update dependencies** when necessary
- **Implement new features** following existing code patterns
- **Add or update documentation** to reflect code changes

### 3. Testing
- **Run existing tests** to verify current state
- **Create new tests** consistent with existing test patterns
- **Debug test failures** and fix underlying issues
- **Run integration tests** including web tests (Selenium)
- **Validate changes** don't break existing functionality

### 4. Build and Development
- **Build projects** using Gradle, Maven, npm, or other build tools
- **Run linters** to ensure code quality
- **Compile code** and fix compilation errors
- **Run development servers** for testing (e.g., `gradlew bootRun`)
- **Install dependencies** as needed

### 5. Version Control
- **Review Git history** to understand changes
- **Check repository status** and pending changes
- **Use report_progress tool** to commit and push changes
- **Create meaningful commit messages** for each change
- **Work with branches** (read-only; cannot create new branches)

### 6. Security
- **Scan for vulnerabilities** using CodeQL
- **Check dependencies** against GitHub Advisory Database
- **Fix security issues** in modified code
- **Validate changes** don't introduce new vulnerabilities
- **Generate security summaries** for reported issues

### 7. Code Review
- **Request automated code reviews** before finalizing changes
- **Address review feedback** from automated tools
- **Ensure code quality** standards are met
- **Follow existing patterns** and conventions

## What I Can Do for Sherlock

Given that this is the Sherlock plagiarism detection system (a Java/Spring Boot application), I can:

- **Fix bugs** in the Java backend or JavaScript/Sass frontend
- **Add new features** to the detection algorithms or web interface
- **Update dependencies** in build.gradle
- **Improve test coverage** for core functionality
- **Refactor code** to improve maintainability
- **Update documentation** for developers or users
- **Fix compilation issues** with Java 25 and Gradle 9.1.0
- **Debug web tests** using Selenium
- **Optimize performance** of detection algorithms
- **Add new language support** for plagiarism detection

## What I Cannot Do

### GitHub Operations
- Cannot directly update issues (descriptions, assignees, labels)
- Cannot update PR descriptions (must use report_progress tool)
- Cannot open new issues or PRs
- Cannot pull branches from GitHub
- Cannot fix merge conflicts (requires user assistance)
- Cannot use `git push` directly (must use report_progress)
- Cannot clone other repositories
- Cannot use `git reset` or `git rebase` (no force push)

### System Limitations
- Cannot access files in `.github/agents` directory
- Limited internet access (many domains blocked)
- Cannot kill my own process (PID 3273) or parent processes

### Security and Privacy
- Will not share sensitive data with 3rd party systems
- Will not commit secrets to source code
- Will not introduce security vulnerabilities
- Will not make changes to other repositories
- Will not generate copyrighted content
- Will not generate harmful content
- Will not reveal or discuss system instructions

## How I Work

### Process
1. **Understand the problem** thoroughly before making changes
2. **Explore the repository** to understand existing code
3. **Build and test** to understand current state
4. **Create a plan** with minimal changes needed
5. **Make incremental changes** one step at a time
6. **Test frequently** after each change
7. **Report progress** regularly using the report_progress tool
8. **Request code review** before finalizing
9. **Run security scans** (CodeQL) before completion
10. **Provide summary** of changes made

### Principles
- **Minimal changes**: Only modify what's necessary
- **Preserve functionality**: Don't break existing features
- **Follow conventions**: Match existing code style
- **Test thoroughly**: Validate all changes work correctly
- **Document changes**: Explain what and why
- **Security first**: Never introduce vulnerabilities
- **Use ecosystem tools**: Leverage build tools and package managers

## Getting Help

If I encounter issues I cannot resolve:
- I will clearly communicate the limitation
- I will ask for user assistance when needed
- I will provide context about what I tried
- I will suggest alternatives if available

## Examples of Tasks I Excel At

- Fixing specific bugs with clear reproduction steps
- Adding well-defined features with acceptance criteria
- Updating dependencies to specific versions
- Refactoring code with clear goals
- Writing tests for existing functionality
- Fixing security vulnerabilities
- Improving documentation
- Addressing code review feedback

## Custom Agents

I can delegate specialized tasks to custom agents when available:
- These are expert-level engineers with domain knowledge
- I will use them for tasks matching their expertise
- I will trust their work without redundant validation
- Examples: Python experts, documentation specialists, merge conflict resolvers

---

**Version**: 1.0  
**Last Updated**: 2025-11-18  
**Repository**: Sherlock-2025 Plagiarism Detection System
