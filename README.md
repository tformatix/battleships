# Battleships
Android App of the game Battleships (German: Schiffe versenken) <br/>
Project for Mobile Computing study in University of Applied Sciences Austria Campus Hagenberg
## Game
* strategy guessing game
* played on grids
  * ships with different sizes are marked on the grid
* location of the opponent’s ships must be guessed
  * player “shoots” on ships
  * a ship is sunken, when the player located the whole ship
* if every of the opponent’s ships are sunken
  * player won
## Rules
* one ship must be built in one row or one column
  * not diagonal
* ships must not collide with each other
  * one cell at minimum has to be free between every ship
## Features
* offline multiplayer
  * playing on only one phone
* selecting number of ships used in the game
  * available ship sizes: 5, 4, 3 and 2
  * 0 to 5 ships per size
* selecting board size (number of rows and columns)
  * 5 to 20 rows/columns
  * for example: 10x10 field
* checking wrong user input
  * colliding ships, unavailable ship used, …
## Project Structure
### Overview
![grafik](https://user-images.githubusercontent.com/45870302/124254729-a434d500-db29-11eb-9616-6e4b0ae0962e.png)
### Background Logic
![grafik](https://user-images.githubusercontent.com/45870302/124254255-2a045080-db29-11eb-93ed-db54a154c9b6.png)
