# Dynameq to MATSim Translation Project

## Purpose
The primary objective of this project is to translate input files from Dynameq format to a format suitable for use in MATSim.

## Quick Start
1. Begin by cloning the entire repo: `git clone https://github.com/vtisweden/matsim-projects.git`

2. Import and Set Up the Project
- Setup project according to Maven instructions. Working in Eclipse IDE: Import the cloned repo as an existing Maven project.
- Ensure that all dependencies are properly resolved and the project builds without errors.

3. Running the Translation Scripts
- There are two primary files responsible for the translation process: `CreateNetwork.java` and `CreatePopulation.java`.
- Always run `CreateNetwork.java` before `CreatePopulation.java` to ensure the data flows correctly.
- Prior to running these scripts, adjust the file paths within each script to correctly point to your input data locations. More detailed instructions on how to adjust these paths can be found within the respective files.

4. Execute the Translation
Once everything is set up, execute the scripts in the aforementioned order to complete the translation process.
