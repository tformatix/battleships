package at.fhooe.mc.android.battleships

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import at.fhooe.mc.android.battleships.databinding.ActivityMainBinding
import at.fhooe.mc.android.battleships.logic.*
import kotlin.math.abs

const val TAG: String = "Battleships" // tag for logging output

/**
 * Battleship App by @author Tobias Fischer
 * Project for Mobile Computing study in University of Applied Sciences Austria Campus Hagenberg
 */
class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var battleGround1: BattleGround // battle ground of player 1
    private lateinit var battleGround2: BattleGround // battle ground of player 2
    private lateinit var currentBattleGround: BattleGround // currently selected player ground
    private lateinit var cellState2Color: MutableMap<CellState, Int> // mapping cell state to colors
    private lateinit var initAvailableShips: MutableMap<Int, Int> // init map available ships
    private var settingsDialog: Dialog? = null // settings dialog
    private var tableSize: Int = 0 // init table size
    private var currentShip: Ship? = null // current ship for inserting
    private var rectSize: Int = 0 // size of one cell
    private var shootAllowed: Boolean = true // more than 1 shoot per round are not allowed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

    }

    /**
     * init app
     */
    private fun init(){
        initAvailableShips = mutableMapOf(
            5 to getString(R.string.dialog_settings_default5).toInt(),
            4 to getString(R.string.dialog_settings_default4).toInt(),
            3 to getString(R.string.dialog_settings_default3).toInt(),
            2 to getString(R.string.dialog_settings_default2).toInt()
        ) // default map
        tableSize = getString(R.string.dialog_settings_default_size).toInt() // default size

        binding.activityMainButtonAddSelection.setOnClickListener { addSelectedShip() } // add selected selected ship to batte ground
        binding.activityMainButtonResetSelection.setOnClickListener { resetSelectedShip() } // add remove selected ship
        binding.activityMainButtonNextPlayer.setOnClickListener { changeCurrentBattleGround() } // next player is on turn
        binding.activityMainButtonRestart.setOnClickListener { initGame() } // start new game
        binding.activityMainImgRefresh.setOnClickListener { gameSettings(false) } // game settings

        initCellState2Color()

        gameSettings(true)
    }

    /**
     * CellState to Color map is filled (colors are stored in colors.xml)
     */
    private fun initCellState2Color() {
        cellState2Color = mutableMapOf(
            CellState.SHIP to getColor(R.color.color_cellstate_ship),
            CellState.SUNK to getColor(R.color.color_cellstate_sunk),
            CellState.HIT to getColor(R.color.color_cellstate_hit),
            CellState.MISS to getColor(R.color.color_cellstate_miss),
            CellState.WATER to getColor(R.color.color_cellstate_water),
            CellState.ERROR to getColor(R.color.color_cellstate_error),
            CellState.SELECTED to getColor(R.color.color_cellstate_selected)
        )
    }

    /**
     * @param gameStart: true if the beginning of game; false if cancelable
     * open game settings dialog and restart the game
     * availableShips and tableSize are set
     */
    private fun gameSettings(gameStart: Boolean) {
        val bob: AlertDialog.Builder = AlertDialog.Builder(this)
        with(bob) {
            setView(R.layout.dialog_settings)
            setPositiveButton(android.R.string.ok) { _, _ ->
                fillValuesSettings()
                validityCheckSettings(gameStart)
            }
            if (!gameStart) {
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    Toast.makeText(this@MainActivity, getString(R.string.dialog_settings_toast_canceled), Toast.LENGTH_LONG).show()
                }
            }
            settingsDialog = create()
            settingsDialog?.show()
        }
    }

    /**
     * fill corresponding values of settings dialog
     */
    private fun fillValuesSettings() {
        // available ships
        val edit5: EditText = settingsDialog!!.findViewById(R.id.dialog_settings_edit_5)
        initAvailableShips[5] = edit5.text.toString().toInt()
        val edit4: EditText = settingsDialog!!.findViewById(R.id.dialog_settings_edit_4)
        initAvailableShips[4] = edit4.text.toString().toInt()
        val edit3: EditText = settingsDialog!!.findViewById(R.id.dialog_settings_edit_3)
        initAvailableShips[3] = edit3.text.toString().toInt()
        val edit2: EditText = settingsDialog!!.findViewById(R.id.dialog_settings_edit_2)
        initAvailableShips[2] = edit2.text.toString().toInt()
        // table size
        val editTableSize: EditText = settingsDialog!!.findViewById(R.id.dialog_settings_edit_table_size)
        tableSize = editTableSize.text.toString().toInt()
    }

    /**
     * check validity of settings values (ranges)
     */
    private fun validityCheckSettings(gameStart: Boolean) {
        var valid = true
        if (tableSize !in 5..20)
            valid = false
        var countShips: Int = 0
        for (availableShip in initAvailableShips) {
            countShips += availableShip.value
            if (availableShip.value !in 0..5)
                valid = false
        }
        if(countShips <= 0)
            valid = false
        if (valid) {
            Toast.makeText(this@MainActivity, getString(R.string.dialog_settings_toast_success), Toast.LENGTH_LONG).show()
            initGame()
        } else {
            Toast.makeText(this@MainActivity, getString(R.string.dialog_settings_toast_invalid), Toast.LENGTH_LONG).show()
            gameSettings(gameStart)
        }
    }

    /**
     * init new game
     */
    private fun initGame() {
        rectSize = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            windowManager.currentWindowMetrics.bounds.width() / tableSize // Portrait Mode - width is limiting factor
        else
            windowManager.currentWindowMetrics.bounds.height() / tableSize // Landscape Mode - height is limiting factor

        battleGround1 = BattleGround(getString(R.string.battle_ground_player_1), initAvailableShips, tableSize)
        battleGround2 = BattleGround(getString(R.string.battle_ground_player_2), initAvailableShips, tableSize)
        currentBattleGround = battleGround1
        binding.activityMainButtonRestart.visibility = View.INVISIBLE
        binding.activityMainTvGameState.text = getString(R.string.activity_main_tv_game_state_init)

        drawBoard()
    }

    /**
     * dynamically draws battle ground for currentBattleGround
     */
    private fun drawBoard() {
        //Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::drawBoard()")
        displayAvailableShips()
        binding.activityMainTblBoard.removeAllViews()
        binding.activityMainTvCurrentPlayer.text = currentBattleGround.playerName
        val layoutParams = TableRow.LayoutParams(rectSize, rectSize)
        for (i in 0 until tableSize) {
            var tblRow = TableRow(this)
            for (j in 0 until tableSize) {
                tblRow.addView(cell2View(i, j), layoutParams)
            }
            binding.activityMainTblBoard.addView(tblRow)
        }
    }

    /**
     * @return a view (ImageView)
     * dynamically builds a view for a cell (colors stored in colors.xml)
     */
    private fun cell2View(row: Int, col: Int): ImageView {
        val cell: Cell = currentBattleGround.playerBoard[row][col]
        return ImageView(this).apply {
            // drawable_cellstate = default drawable (border already set)
            // cellState2Color = CellState to color map
            setImageDrawable((getDrawable(R.drawable.drawable_cellstate) as GradientDrawable).apply {
                cellState2Color[cell.cellState]?.let { setColor(it) }
            })
            setOnClickListener {
                when (cell.cellState) {
                    CellState.SHIP -> cellStateShipClicked(row, col)
                    CellState.WATER -> cellStateWaterClicked(row, col)
                    CellState.ERROR -> cellStateErrorClicked()
                }
            }
        }
    }

    /**
     * player clicks on SHIP cell
     */
    private fun cellStateShipClicked(row: Int, col: Int) {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::cellStateShipClicked()")
        shoot(row, col)
    }

    /**
     * player clicks on WATER cell
     */
    private fun cellStateWaterClicked(row: Int, col: Int) {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::cellStateWaterClicked()")
        if (currentBattleGround.getGameStarted()) {
            shoot(row, col)
        } else {
            cellStateWaterGameNotStarted(row, col)
        }
    }

    /**
     * ship init (player clicks empty WATER cell when game not started)
     * --> Cell gets SELECTED under following conditions
     *     - no pending ship OR next to pending ship (endRow+1 OR endCol+1)
     *     - checks direction
     *     - top -> bottom OR left to right
     * ship is stored in currentShip
     */
    private fun cellStateWaterGameNotStarted(row: Int, col: Int) {
        // checks if a ship is pending
        if (currentShip == null) {
            currentShip = Ship(row, col, row, col, Direction.UNDEFINED) // create new ship
            currentShip!!.length = 1
            binding.activityMainButtonResetSelection.visibility = View.VISIBLE
        } else {
            // checks if row/col is next to endRow/Col - diagonal and somewhere else not allowed
            if ((abs(currentShip!!.endRow - row) + abs(currentShip!!.endCol - col)) != 1) {
                Toast.makeText(this, getString(R.string.activity_main_toast_invalid_cell), Toast.LENGTH_LONG).show()
                return
            }
            // checks if row/col is greater than endRow/Col - only top to bottom OR left to right available
            if (currentShip!!.endRow > row || currentShip!!.endCol > col) {
                Toast.makeText(this, getString(R.string.activity_main_toast_invalid_cell), Toast.LENGTH_LONG).show()
                return
            }
            binding.activityMainButtonAddSelection.visibility = View.VISIBLE
            // direction checks
            when (currentShip!!.direction) {
                Direction.UNDEFINED -> { // direction not set yet
                    if (currentShip!!.startRow != row) {
                        currentShip!!.direction = Direction.VERTICAL
                    } else {
                        currentShip!!.direction = Direction.HORIZONTAL
                    }
                }
                Direction.HORIZONTAL -> {
                    if (currentShip!!.endRow != row) {
                        Toast.makeText(this, getString(R.string.activity_main_toast_invalid_cell), Toast.LENGTH_LONG).show()
                        return
                    }
                }
                Direction.VERTICAL -> {
                    if (currentShip!!.endCol != col) {
                        Toast.makeText(this, getString(R.string.activity_main_toast_invalid_cell), Toast.LENGTH_LONG).show()
                        return
                    }
                }
            }
            // set params of ship
            currentShip!!.endRow = row
            currentShip!!.endCol = col
            currentShip!!.length++
            currentShip!!.undestroyedCount = currentShip!!.length
        }

        // checks if selection is colliding with a ship
        if (currentBattleGround.setShipChecks(currentShip!!)) {
            currentBattleGround.playerBoard[row][col] = Cell(CellState.SELECTED, null)
            drawBoard()
        } else {
            Toast.makeText(this, getString(R.string.activity_main_toast_ship_check_fail), Toast.LENGTH_LONG).show()
            resetSelectedShip()
        }
    }

    /**
     * some error cell
     */
    private fun cellStateErrorClicked() {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::cellStateErrorClicked()")
        Toast.makeText(this, getString(R.string.activity_main_toast_error), Toast.LENGTH_LONG).show()
    }

    /**
     * add SELECTED ship into player's battleGround
     */
    private fun addSelectedShip() {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::addSelectedShip()")
        if (currentShip == null) {
            Toast.makeText(this, getString(R.string.activity_main_toast_no_selection), Toast.LENGTH_SHORT).show()
        } else {
            if (currentBattleGround.setShip(currentShip!!)) {
                currentShip = null
                binding.activityMainButtonAddSelection.visibility = View.INVISIBLE
                binding.activityMainButtonResetSelection.visibility = View.INVISIBLE
                drawBoard()
            } else {
                Toast.makeText(this, getString(R.string.activity_main_toast_set_ship_fail), Toast.LENGTH_LONG).show()
                resetSelectedShip()
            }
        }
    }

    /**
     * remove SELECTED ship from player's battleGround
     */
    private fun resetSelectedShip() {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::resetSelectShip()")
        if (currentShip == null) {
            Toast.makeText(this, getString(R.string.activity_main_toast_no_selection), Toast.LENGTH_SHORT).show()
            return
        } else if (currentShip!!.direction == Direction.UNDEFINED) {
            currentBattleGround.playerBoard[currentShip!!.endRow][currentShip!!.endCol] = Cell(CellState.WATER, null)
        } else {
            if (!currentBattleGround.removeShip(currentShip!!)) {
                Toast.makeText(this, getString(R.string.activity_main_toast_reset_ship_fail), Toast.LENGTH_LONG).show()
            }
        }
        currentShip = null
        binding.activityMainButtonAddSelection.visibility = View.INVISIBLE
        binding.activityMainButtonResetSelection.visibility = View.INVISIBLE
        drawBoard()
    }

    /**
     * display available ships table
     * checks ending of game
     * - if gameStarted = true --> current player's battle ground lost
     * - otherwise --> game state gets true and other player start init OR game starts
     */
    private fun displayAvailableShips() {
        //Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::displayAvailableShips()")
        if (currentBattleGround.getAvailableShipsCount() > 0) {
            binding.activityMainTblAvailableShips.removeAllViews()

            binding.activityMainTblAvailableShips.addView(
                getTblRowAvailableShips(
                    arrayOf(
                        getString(R.string.activity_main_tv_available_ships_length),
                        getString(R.string.activity_main_tv_available_ships_amount)
                    ), true
                )
            )
            for (availableShip in currentBattleGround.getAvailableShips()) {
                binding.activityMainTblAvailableShips.addView(
                    getTblRowAvailableShips(
                        arrayOf("${availableShip.key}", "${availableShip.value}"),
                        false
                    )
                )
            }
        } else { // no ships available
            if (currentBattleGround.getGameStarted()) { // winner
                handleWin()
            } else { // start game
                currentBattleGround.startGame(true)
                if (currentBattleGround.playerName == battleGround2.playerName) { // game starts now
                    cellState2Color[CellState.SHIP] = getColor(R.color.color_cellstate_water)
                    changeGameMode()
                } else { // other players turn to init
                    changeCurrentBattleGround()
                }
            }
        }
    }

    /**
     * @param strings: a array of strings which get onto the available ships table
     * @param header: is header of table
     * puts TextViews onto a TableRow
     * @return a TableRow
     */
    private fun getTblRowAvailableShips(strings: Array<String>, header: Boolean): TableRow {
        val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
        if (header)
            layoutParams.setMargins(0, 0, 20, 0)
        var tblRow = TableRow(this)
        strings.iterator().forEach {
            var txtView = TextView(this).apply { text = it }
            with(txtView) {
                gravity = Gravity.CENTER
                if (header)
                    setTypeface(null, Typeface.BOLD)
                tblRow.addView(this, layoutParams)
            }
        }
        return tblRow
    }

    /**
     * changes current selected battle ground
     */
    private fun changeCurrentBattleGround() {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::changeCurrentBattleGround()")
        if (currentBattleGround.playerName == battleGround1.playerName)
            currentBattleGround = battleGround2
        else
            currentBattleGround = battleGround1
        binding.activityMainButtonNextPlayer.visibility = View.INVISIBLE
        shootAllowed = true
        drawBoard()
    }

    /**
     * game mode changes (init -> game)
     */
    private fun changeGameMode() {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::changeGameMode()")
        // change names in headline
        val tmpName: String = battleGround1.playerName
        battleGround1.playerName = battleGround2.playerName
        battleGround2.playerName = tmpName
        binding.activityMainTvGameState.text = getString(R.string.activity_main_tv_game_state_shoot)
        drawBoard()
    }

    /**
     * player tries to shoot on a ship
     */
    private fun shoot(row: Int, col: Int) {
        if (shootAllowed) {
            currentBattleGround.shoot(row, col)
            binding.activityMainButtonNextPlayer.visibility = View.VISIBLE // next player
            drawBoard()
            shootAllowed = false
        }
    }

    /**
     * the shooter of currentBattleGround won
     */
    @SuppressLint("StringFormatMatches")
    private fun handleWin() {
        Log.d(TAG, "MainActivity@${currentBattleGround.playerName}::handleWin()")
        cellState2Color[CellState.SHIP] = getColor(R.color.color_cellstate_ship) // loser is allowed to see other ships
        binding.activityMainTvGameState.text = getString(R.string.activity_main_tv_game_state_winner, currentBattleGround.playerName)
        changeCurrentBattleGround()
        binding.activityMainButtonNextPlayer.visibility = View.INVISIBLE
        binding.activityMainButtonRestart.visibility = View.VISIBLE
        Toast.makeText(this, "${currentBattleGround.playerName} won", Toast.LENGTH_LONG).show()
    }
}