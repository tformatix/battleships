package at.fhooe.mc.android.battleships.logic

class Cell(var cellState: CellState, var ship: Ship?) {
    /**
     * the states SHIP, HIT, SUNK don't allow a empty ship
     */
    init {
        if (cellState in arrayOf(CellState.SHIP, CellState.HIT, CellState.SUNK) && ship == null)
            cellState = CellState.ERROR
    }

    override fun toString(): String {
        return "$cellState::$ship"
    }
}