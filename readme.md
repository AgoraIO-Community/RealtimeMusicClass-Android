# Realtime Music Class

点击[此处](readme.CN.md)获取中文说明

## Create your own music list
### Creating a music config list
Create a json file named **musics.json**, put the following content to the file as below

````json
    [{
      "name": "music1",
      "identifier": "0001",
      "music": "music1.mp3",
      "lyric": "music1.xml"
    }, {
      "name": "music2",
      "identifier": "0002",
      "music": "music2.mp3",
      "lyric": "music2.xml"
    }]
````

Each music has four properties:
* **name**: name of the music shown in the music list
* **identifier**: the id of the music(currently not used in the demo), ought to unique to project's needs
* **music**: music file name with the corresponding extension file name, multiple music formats are supported
* **lyric**: lyric file name with the proper extension file name, multiple lyric formats are supported

The musics in the config list can be as many as needed

### Preparing your own music and lyric files

Copy your config file, music and lyric files to the project directory

**app/src/main/assets**

### Run the project
Once a proper app id is provided, and the demo is started correctly, demo reads the config file and show the music list in the console.