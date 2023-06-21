# SnowRunnerTool
This is a small tool for the game [SnowRunner](https://store.steampowered.com/app/1465360/SnowRunner).  
It reads all data of usable trucks, trailers and addons from game data ([SnowRunner]/preload/paks/client/initial.pak) and also parses savegame data.

### Download
You can download my releases [here](https://github.com/Hendrik2319/SnowRunnerTool/releases).
You will need a JAVA 17 VM.

### Development
This project is currently configured as an Eclipse project. The libraries, it depends on, are imported as projects into java build path via Eclipse.
This is more convenient, when changes in SnowRunnerTool need changes in the libraries.

### Dependencies
Following libraries are currently imported as projects into java build path via Eclipse.
* [`JavaLib_Common_Dialogs`       ](https://github.com/Hendrik2319/JavaLib_Common_Dialogs)
* [`JavaLib_Common_Essentials`    ](https://github.com/Hendrik2319/JavaLib_Common_Essentials)
* [`JavaLib_Common_HSColorChooser`](https://github.com/Hendrik2319/JavaLib_Common_HSColorChooser)
* [`JavaLib_JSON_Parser`          ](https://github.com/Hendrik2319/JavaLib_JSON_Parser)

If you want to develop for your own and
* you use Eclipse as IDE,
	* then you should clone the projects above too and add them to the same workspace as the SnowRunnerTool project.
* you use another IDE (e.q. VS Code)
	* then you should clone the said projects, build JAR files of them and add the JAR files as libraries.

### Screenshots

Truck Table  
<img src="/github/screenshot1_main.png" alt="Truck table" title="Truck Table" width="300"/>

Usable addons of a truck  
<img src="/github/screenshot2_addons.png" alt="Usable addons of a truck" title="Usable addons of a truck" width="300"/>

Usable trailers of a truck  
<img src="/github/screenshot3_trailers.png" alt="Usable trailers of a truck" title="Usable trailers of a truck" width="300"/>

Save game view: Stored trucks  
<img src="/github/screenshot4_savegame_storedtrucks.png" alt="Save game view: Stored trucks" title="Save game view: Stored trucks" width="300"/>
