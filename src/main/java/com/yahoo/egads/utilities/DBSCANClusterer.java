/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.egads.utilities;
 
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterer;

/**
 * DBSCAN (density-based spatial clustering of applications with noise) algorithm.
 * <p>
 * The DBSCAN algorithm forms clusters based on the idea of density connectivity, i.e.
 * a point p is density connected to another point q, if there exists a chain of
 * points p<sub>i</sub>, with i = 1 .. n and p<sub>1</sub> = p and p<sub>n</sub> = q,
 * such that each pair <p<sub>i</sub>, p<sub>i+1</sub>> is directly density-reachable.
 * A point q is directly density-reachable from point p if it is in the ε-neighborhood
 * of this point.
 * <p>
 * Any point that is not density-reachable from a formed cluster is treated as noise, and
 * will thus not be present in the result.
 * <p>
 * The algorithm requires two parameters:
 * <ul>
 *   <li>eps: the distance that defines the ε-neighborhood of a point
 *   <li>minPoints: the minimum number of density-connected points required to form a cluster
 * </ul>
 *
 * @param <T> type of the points to cluster
 * @see <a href="http://en.wikipedia.org/wiki/DBSCAN">DBSCAN (wikipedia)</a>
 * @see <a href="http://www.dbs.ifi.lmu.de/Publikationen/Papers/KDD-96.final.frame.pdf">
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise</a>
 * @version $Id: DBSCANClusterer.java 1461866 2013-03-27 21:54:36Z tn $
 * @since 3.2
 */
@Slf4j
public class DBSCANClusterer<T extends Clusterable> extends Clusterer<T> {
 
    /** Maximum radius of the neighborhood to be considered. */
    // 要考虑邻域的最大半径
    private final double              eps;
 
    /** Minimum number of points needed for a cluster. */
    // 聚类所需的最小点数
    private final int                 minPts;
 
    /** Status of a point during the clustering process. */
    private enum PointStatus {
        /** The point has is considered to be noise. */
        NOISE,
        /** The point is already part of a cluster. */
        PART_OF_CLUSTER
    }
 
    /**
     * Creates a new instance of a DBSCANClusterer.
     * <p>
     * The euclidean distance will be used as default distance measure.
     *
     * @param eps maximum radius of the neighborhood to be considered
     * @param minPts minimum number of points needed for a cluster
     * @throws NotPositiveException if {@code eps < 0.0} or {@code minPts < 0}
     */
    public DBSCANClusterer(final double eps, final int minPts)
        throws NotPositiveException {
        // TODO DBSCAN这里的距离度量算法可以做修改
        this(eps, minPts, new EuclideanDistance());
    }
 
    /**
     * Creates a new instance of a DBSCANClusterer.
     *
     * @param eps maximum radius of the neighborhood to be considered
     * @param minPts minimum number of points needed for a cluster
     * @param measure the distance measure to use
     * @throws NotPositiveException if {@code eps < 0.0} or {@code minPts < 0}
     */
    public DBSCANClusterer(final double eps, final int minPts, final DistanceMeasure measure)
        throws NotPositiveException {
        // 初始化父类的距离度量算法，因为本类继承了父类的distance方法
        super(measure);
 
        if (eps < 0.0d) {
            throw new NotPositiveException(eps);
        }
        if (minPts < 0) {
            throw new NotPositiveException(minPts);
        }
        this.eps = eps;
        this.minPts = minPts;
    }
 
    /**
     * Returns the maximum radius of the neighborhood to be considered.
     * @return maximum radius of the neighborhood
     */
    public double getEps() {
        return eps;
    }
 
    /**
     * Returns the minimum number of points needed for a cluster.
     * @return minimum number of points needed for a cluster
     */
    public int getMinPts() {
        return minPts;
    }
 
    /**
     * Performs DBSCAN cluster analysis.
     * points里面的每一个point，存放了点在时序数据的中索引和点对应的误差统计数据集合d[]
     *
     * @param points the points to cluster
     * @return the list of clusters
     * @throws NullArgumentException if the data points are null
     */
    public List<Cluster<T>> cluster(final Collection<T> points) throws NullArgumentException {
 
        // sanity checks
        MathUtils.checkNotNull(points);
        // 返回所有的聚类
        final List<Cluster<T>> clusters = new ArrayList<Cluster<T>>();
        // 存放一个异常cluster
        final List<Cluster<T>> anomalousClusters = new ArrayList<Cluster<T>>();
        // 所有的异常点聚到一起
        final Cluster<T> anomalyCluster = new Cluster<T>();
        final Map<Clusterable, PointStatus> visited = new HashMap<Clusterable, PointStatus>();

        // 遍历所有的误差统计数据点
        for (final T point : points) {
            if (visited.get(point) != null) {
                continue;
            }
            final List<T> neighbors = getNeighbors(point, points);
            if (neighbors.size() >= minPts) { // 核心点
                // DBSCAN does not care about center points
                final Cluster<T> cluster = new Cluster<T>(); // 这个cluster变量用于记录点并返回
                clusters.add(/*返回上边这个final cluster*/expandCluster(cluster, point/*点本身*/, neighbors/*点的邻居*/, points, visited/*访问记录*/));
            } else { // 非核心点
                visited.put(point, PointStatus.NOISE);
                anomalyCluster.addPoint(point);
            }
        }
        anomalousClusters.add(anomalyCluster);
        return anomalousClusters;
    }

    public List<Cluster<T>> cluster_getClusters_MY(final Collection<T> points, int type) throws NullArgumentException {
        int total = 0;
        // sanity checks
        MathUtils.checkNotNull(points);
        // 返回所有的聚类
        final List<Cluster<T>> clusters = new ArrayList<Cluster<T>>();
        // 存放一个异常cluster
        final List<Cluster<T>> anomalousClusters = new ArrayList<Cluster<T>>();
        // 所有的异常点聚到一起
        final Cluster<T> anomalyCluster = new Cluster<T>();
        final Map<Clusterable, PointStatus> visited = new HashMap<Clusterable, PointStatus>();

        // 遍历所有的误差统计数据点
        for (final T point : points) {
            if (visited.get(point) != null) {
                continue;
            } // 重集合Points中抽取一个未处理的点
            final List<T> neighbors = getNeighbors(point, points); // 寻找可达的点
            if (neighbors.size() /*邻居个数*/ >= minPts) { // 如果可达点的个数大于最小聚类点个数，则这个该点为 核心点，然后point+neiberhoods组成一个簇
                // DBSCAN does not care about center points
                final Cluster<T> cluster = new Cluster<T>();
                clusters.add(expandCluster(cluster, point, neighbors, points, visited));
                total += cluster.getPoints().size();
            } else {
                visited.put(point, PointStatus.NOISE);
                anomalyCluster.addPoint(point);
            }
        }
        anomalousClusters.add(anomalyCluster);
        total += anomalyCluster.getPoints().size();
        // 噪声
//        if (anomalyCluster.getPoints().size() != 0) {
//            clusters.add(anomalyCluster);
//        }
//        log.info("cluster 方法中 points.zise:{} 聚类的点数:{}", points.size(), total);
        if (type == 0) {
            return clusters;
        }
        return anomalousClusters;
    }
 
    /**
     * Expands the cluster to include density-reachable items.
     *
     * @param cluster Cluster to expand
     * @param point Point to add to cluster
     * @param neighbors List of neighbors
     * @param points the data set
     * @param visited the set of already visited points
     * @return the expanded cluster
     */
    private Cluster<T> expandCluster(final Cluster<T> cluster,
                                     final T point/*点本身*/,
                                     final List<T> neighbors/*点的邻居*/,
                                     final Collection<T> points/*所有点的集合*/,
                                     final Map<Clusterable, PointStatus> visited/*点的访问记录*/) {
        cluster.addPoint(point); // 加入核心点
        visited.put(point, PointStatus.PART_OF_CLUSTER);
 
        List<T> seeds = new ArrayList<T>(neighbors);
        int index = 0;
        while (index < seeds.size()) { // 遍历所有的邻居
            final T current = seeds.get(index); // 获取一个邻居点
            PointStatus pStatus = visited.get(current); // 设置访问标记
            // only check non-visited points
            if (pStatus == null) {
                final List<T> currentNeighbors = getNeighbors(current, points);
                if (currentNeighbors.size() >= minPts) {
                    seeds = merge(seeds, currentNeighbors);
                }
            }
 
            if (pStatus != PointStatus.PART_OF_CLUSTER) {
                visited.put(current, PointStatus.PART_OF_CLUSTER);
                cluster.addPoint(current); // 加入邻居
            }
 
            index++;
        }
        return cluster;
    }
 
    /**
     * Returns a list of density-reachable neighbors of a {@code point}.
     * 寻找以point为原点，半径为eps的点，组成集合neighbors（不包括自己）
     *
     * @param point the point to look for
     * @param points possible neighbors
     * @return the List of neighbors
     */
    private List<T> getNeighbors(final T point, final Collection<T> points) {
        final List<T> neighbors = new ArrayList<T>();
        for (final T neighbor/*邻居节点也是point类型*/ : points) {
            // 距离度量，计算误差统计指标数组之间的距离
            if (point != neighbor/*除开自己*/ && distance(neighbor, point) <= eps) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }
 
    /**
     * Merges two lists together.
     *
     * @param one first list
     * @param two second list
     * @return merged lists
     */
    private List<T> merge(final List<T> one, final List<T> two) {
        final Set<T> oneSet = new HashSet<T>(one);
        for (T item : two) {
            if (!oneSet.contains(item)) {
                one.add(item);
            }
        }
        return one;
    }
}