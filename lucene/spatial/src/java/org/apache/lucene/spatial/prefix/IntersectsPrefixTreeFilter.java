package org.apache.lucene.spatial.prefix;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.SpatialRelation;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.spatial.prefix.tree.Node;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;

/**
 * A Filter matching documents that have an {@link SpatialRelation#INTERSECTS}
 * (i.e. not DISTINCT) relationship with a provided query shape.
 *
 * @lucene.internal
 */
public class IntersectsPrefixTreeFilter extends AbstractVisitingPrefixTreeFilter {

  private final boolean hasIndexedLeaves;

  public IntersectsPrefixTreeFilter(Shape queryShape, String fieldName,
                                    SpatialPrefixTree grid, int detailLevel,
                                    int prefixGridScanLevel, boolean hasIndexedLeaves) {
    super(queryShape, fieldName, grid, detailLevel, prefixGridScanLevel);
    this.hasIndexedLeaves = hasIndexedLeaves;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && hasIndexedLeaves == ((IntersectsPrefixTreeFilter)o).hasIndexedLeaves;
  }

  @Override
  public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
    return new VisitorTemplate(context, acceptDocs, hasIndexedLeaves) {
      private OpenBitSet results;

      @Override
      protected void start() {
        results = new OpenBitSet(maxDoc);
      }

      @Override
      protected DocIdSet finish() {
        return results;
      }

      @Override
      protected boolean visit(Node cell) throws IOException {
        if (cell.getShapeRel() == SpatialRelation.WITHIN || cell.getLevel() == detailLevel) {
          collectDocs(results);
          return false;
        }
        return true;
      }

      @Override
      protected void visitLeaf(Node cell) throws IOException {
        collectDocs(results);
      }

      @Override
      protected void visitScanned(Node cell, Shape cellShape) throws IOException {
        if (queryShape.relate(cellShape).intersects())
          collectDocs(results);
      }

    }.getDocIdSet();
  }

}
