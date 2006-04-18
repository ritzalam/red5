import com.gskinner.events.GDispatcher;
//import com.acmewebworks.utils.CoordinateTools

class org.red5.utils.GridManager
{
	private static var eventDispatcherDependency:Object = GDispatcher;
	private static var eventDispatcherInitialized:Boolean = initEventDispatcher();

	public static var addEventListener:Function;
	public static var removeEventListener:Function;
	private static var dispatchEvent:Function;

	public static var mc:MovieClip; // movieclip where grid is created
	public static var cols:Number;
	public static var rows:Number;
	public static var gridCount:Number; // zero based number of tiles
	public static var currentGridLocation:Number; // represents current gridLocation
	private static var lastGridLocation:Number; // represents last gridLocation

	public static var horizontalRatio:Number;
	public static var verticalRatio:Number;


	public function onLoad()
	{
		init();
	}

	public static function init():Void
	{
		if(!GridManager.cols) GridManager.cols = 8;
		if(!GridManager.rows) GridManager.rows = 8;
	}

	private static function initEventDispatcher():Boolean
	{
		// initialize the Connector to dispatch events
		GDispatcher.initialize(GridManager);
		GridManager.init();
		return true;
	}

	public static function initGrid(p_mc:MovieClip, p_cols:Number, p_rows:Number, p_gridWidth:Number, p_gridHeight:Number, p_lineSize:Number, p_color:Number, p_drawGrid:Boolean):Void
	{
		_global.tt("initGrid called", arguments);
		/*
		NOTES:
			Draws a grid using the drawing api:
			initGrid(13,6,600,400,true);
		*/
		mc = p_mc;
		cols = p_cols;
		rows = p_rows;
		gridCount = (cols*rows)-1;
		horizontalRatio = (p_gridWidth)/cols;
		verticalRatio = (p_gridHeight)/rows;
		trace("dimensions :: " + horizontalRatio + " :: " + verticalRatio);
		
		Mouse.addListener(GridManager);

		if(!p_drawGrid) return;

		for(var i:Number=0;i<=cols;i++)
		{
			var line:MovieClip = mc.createEmptyMovieClip("colLine_" + i, mc.getNextHighestDepth());
			line.lineStyle(p_lineSize, p_color, 100);
			line.moveTo(horizontalRatio*i, 0);
			line.lineTo(horizontalRatio*i, p_gridHeight);
		}
		for(var i:Number=0;i<=rows;i++)
		{
			var line:MovieClip = mc.createEmptyMovieClip("rowLine_" + i, mc.getNextHighestDepth());
			line.lineStyle(p_lineSize, p_color, 100);
			line.moveTo(0, verticalRatio*i);
			line.lineTo(p_gridWidth,verticalRatio*i);
		}
	}
	
	public static function calcCenterSpot(p_col:Number, p_row:Number):Object
	{
		var x:Number = (p_col * horizontalRatio) + (horizontalRatio / 2);
		var y:Number = (p_row * verticalRatio) + (verticalRatio / 2);
		
		return {x:x, y:y};
	}
	
	public static function getColRow(p_location):Object
	{
		var row:Number = Math.floor(p_location / cols);
		var col:Number = Math.floor(p_location%cols);;
		
		return {col:col, row:row};
	}

	public static function calcGridLocation(p_x:Number, p_y:Number):Number
	{
		var x:Number = Math.ceil(p_x / (horizontalRatio));
		var y:Number = Math.ceil(p_y / (verticalRatio));

		var gridLocation = (x+(cols*(y-1)))-1; // -1 to make it zero based

		// check to see if the location is outside the bounds of the grid. If so, return null
		if(gridLocation < 0 ||
		   gridLocation > (cols*rows)-1 ||
		   x > cols ||
		   y > rows)
		   {
			   GridManager.currentGridLocation = null; // we want currentGridLocation to have a null if it's out of bounds
			   return null;
		   }
		GridManager.lastGridLocation = GridManager.currentGridLocation;
		GridManager.currentGridLocation = gridLocation; // we want currentGridLocation to have a null if it's out of bounds
		return gridLocation;
	}
	
	private static function onMouseMove():Void
	{
		var location:Number = calcGridLocation(mc._xmouse, mc._ymouse);
		if(location != null) 
		{
			if(location != GridManager.lastGridLocation) dispatchEvent({type:"gridLocationChange", location: location});
			if(location == GridManager.lastGridLocation) dispatchEvent({type:"gridLocation", location: location});
		}
	}
}
