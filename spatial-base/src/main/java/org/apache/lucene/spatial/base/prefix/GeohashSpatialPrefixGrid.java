package org.apache.lucene.spatial.base.prefix;

import org.apache.lucene.spatial.base.context.SpatialContext;
import org.apache.lucene.spatial.base.shape.BBox;
import org.apache.lucene.spatial.base.shape.Point;
import org.apache.lucene.spatial.base.shape.Shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A SpatialPrefixGrid based on Geohashes.  Uses {@link GeohashUtils} to do all the geohash work.
 */
public class GeohashSpatialPrefixGrid extends SpatialPrefixGrid {

  public GeohashSpatialPrefixGrid(SpatialContext shapeIO, int maxLevels) {
    super(shapeIO, maxLevels);
    int MAXP = getMaxLevelsPossible();
    if (maxLevels <= 0 || maxLevels > MAXP)
      throw new IllegalArgumentException("maxLen must be [1-"+MAXP+"] but got "+ maxLevels);
  }

  /** Any more than this and there's no point (double lat & lon are the same). */
  public static int getMaxLevelsPossible() { return GeohashUtils.MAX_PRECISION; }

  @Override
  public int getLevelForDistance(double dist) {
    final int level = GeohashUtils.lookupHashLenForWidthHeight(dist, dist);
    return Math.max(Math.min(level, maxLevels), 1);
  }

  @Override
  public Cell getCell(Point p, int level) {
    return new GhCell(GeohashUtils.encode(p.getY(),p.getX(), level));//args are lat,lon (y,x)
  }

  @Override
  public Cell getCell(String token) {
    return new GhCell(token);
  }

  @Override //for performance
  public Point getPoint(String token) {
    if (token.length() < maxLevels)
      return null;
    return GeohashUtils.decode(token,shapeIO);
  }

  @Override
  public List<Cell> getCells(Shape shape, int detailLevel, boolean inclParents) {
    if (shape instanceof Point)
      return super.getCellsAltPoint((Point) shape, detailLevel, inclParents);
    else
      return super.getCells(shape, detailLevel, inclParents);
  }

  class GhCell extends SpatialPrefixGrid.Cell {
    public GhCell(String token) {
      super(token);
    }

    @Override
    public Collection<SpatialPrefixGrid.Cell> getSubCells() {
      String[] hashes = GeohashUtils.getSubGeohashes(getGeohash());//sorted
      ArrayList<SpatialPrefixGrid.Cell> cells = new ArrayList<SpatialPrefixGrid.Cell>(hashes.length);
      for (String hash : hashes) {
        cells.add(new GhCell(hash));
      }
      return cells;
    }

    @Override
    public int getSubCellsSize() {
      return 32;//8x4
    }

    @Override
    public Cell getSubCell(Point p) {
      return GeohashSpatialPrefixGrid.this.getCell(p,getLevel()+1);//not performant!
    }

    private BBox shape;//cache

    @Override
    public BBox getShape() {
      if (shape == null)
        shape = GeohashUtils.decodeBoundary(getGeohash(), shapeIO);// min-max lat, min-max lon
      return shape;
    }

    private String getGeohash() {
      return token;
    }

  }

}
