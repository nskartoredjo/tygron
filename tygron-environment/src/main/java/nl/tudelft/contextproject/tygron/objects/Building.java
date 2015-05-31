package nl.tudelft.contextproject.tygron.objects;

import com.esri.core.geometry.Polygon;

import nl.tudelft.contextproject.util.PolygonUtil;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Building {
  
  private static final Logger logger = LoggerFactory.getLogger(Building.class);
  
  private int id;
  private String name;
  private Polygon polygon;
  private int floors;
  private String state;
  
  /**
   * Constructs Buildings from a JSONObject.
   * @param input A JSONObject containing building information.
   */
  public Building(JSONObject input) {
    name = input.getString("name");
    id = input.getInt("id");
    try {
      polygon = PolygonUtil.createPolygonFromWkt(input.getString("polygons"));
    } catch (Exception e) {
      logger.info("Error parsing Building with string " + input.toString());
      throw new RuntimeException(e);
    }
    floors = input.getInt("floors");
    state = input.getString("state");
  }

  /**
   * Get the buildings id.
   * @return Building id.
   */
  public int getId() {
    return id;
  } 
  
  /**
   * Get the buildings name.
   * @return Building name.
   */
  public String getName() {
    return name;
  }
  
  /**
   * Get the polygon defining the building.
   * @return Building's polygon.
   */
  public Polygon getPolygon() {
    return polygon;
  }
  
  /**
   * Get the number of floors of the building.
   * @return Number of floors.
   */
  public int getFloors() {
    return floors;
  }
  
  /**
   * Get the state of the building.
   * @return The building's state.
   */
  public String getState() {
    return state;
  }
  
  @Override
  public String toString() {
    JSONObject str = new JSONObject();
    str.put("id", this.id);
    str.put("name", this.name);
    return str.toString();
  }
}
