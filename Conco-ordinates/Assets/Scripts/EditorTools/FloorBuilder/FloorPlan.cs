using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Assertions;
using static UnityEngine.Rendering.VolumeComponent;

[Serializable]
public struct FloorPoint
{
    public int index;
    public Vector2 position;

    public FloorPoint(int index, Vector2 position)
    {
        this.index = index;
        this.position = position;
    }
}

[Serializable]
public struct FloorWall
{
    public int index;
    public int indexA;
    public int indexB;

    public FloorWall(int index, int indexA, int indexB)
    {
        this.index = index;
        this.indexA = indexA;
        this.indexB = indexB;
    }
}

[Serializable]
public struct FloorTransition
{
    public string floorNameA;
    public int wallIndexA;
    public string floorNameB;
    public int wallIndexB;

    public FloorTransition(string floorNameA, int wallIndexA, string floorNameB, int wallIndexB)
    {
        this.floorNameA = floorNameA;
        this.wallIndexA = wallIndexA;
        this.floorNameB = floorNameB;
        this.wallIndexB = wallIndexB;
    }
}

[Serializable]
public class Floor
{
    public string name;
    public List<FloorPoint> points = new List<FloorPoint>();
    public List<FloorWall> walls = new List<FloorWall>();
    public int nextPointID = 0;
    public int nextWallID = 0;

    public Floor(string name)
    {
        this.name = name;
        this.points = new List<FloorPoint>();
        this.walls = new List<FloorWall>();
    }

    public FloorPoint AddPoint(Vector2 pos)
    {
        var point = new FloorPoint(nextPointID, pos);
        nextPointID++;
        points.Add(point);
        return point;
    }

    public FloorWall AddPoint(int indexA, int indexB)
    {
        Assert.IsTrue(points.Exists(x => x.index == indexA));
        Assert.IsTrue(points.Exists(x => x.index == indexB));
        var point = new FloorWall(nextWallID, indexA, indexB);
        nextWallID++;
        walls.Add(point);
        return point;
    }

    public void RemovePoint(int pointIndex)
    {
        walls.RemoveAll(w => w.indexA == pointIndex || w.indexB == pointIndex);

        points.RemoveAll(p => p.index == pointIndex);
    }

    public FloorPoint Point(int index)
    {
        return points.Find(x => x.index == index);
    }

    public FloorWall Wall(int index)
    {
        return walls.Find(x => x.index == index);
    }

    public FloorPoint? FindNearestPoint(Vector2 position, float maxDistance)
    {
        FloorPoint? nearest = null;
        float nearestDist = maxDistance;

        foreach (var point in points)
        {
            float dist = Vector2.Distance(position, point.position);
            if (dist < nearestDist)
            {
                nearestDist = dist;
                nearest = point;
            }
        }

        return nearest;
    }

    public FloorWall? FindNearestWall(Vector2 position, float maxDistance)
    {
        FloorWall? nearest = null;
        float nearestDist = maxDistance;

        foreach (var wall in walls)
        {
            var pointA = Point(wall.indexA);
            var pointB = Point(wall.indexB);

            float dist = DistanceToLineSegment(position, pointA.position, pointB.position);
            if (dist < nearestDist)
            {
                nearestDist = dist;
                nearest = wall;
            }
        }

        return nearest;
    }

    private float DistanceToLineSegment(Vector2 point, Vector2 p1, Vector2 p2)
    {
        Vector2 lineVec = p2 - p1;
        float lengthSquared = lineVec.sqrMagnitude;

        float t = Mathf.Clamp01(Vector2.Dot(point - p1, lineVec) / lengthSquared);
        Vector2 projection = p1 + t * lineVec;

        return Vector2.Distance(point, projection);
    }
}

[CreateAssetMenu(fileName = "FloorPlan", menuName = "Floor Plan Tool/FloorPlan")]
public class FloorPlan : ScriptableObject
{
    [SerializeField]
    private Dictionary<string, Floor> floors = new Dictionary<string, Floor>();

    [SerializeField]
    public List<FloorTransition> FloorTransitions = new List<FloorTransition>();
    public IReadOnlyDictionary<string, Floor> Floors => floors;

    public Floor CreateFloor(string floorName)
    {
        if (floors.ContainsKey(floorName))
        {
            Debug.LogWarning($"Floor '{floorName}' already exists!");
            return floors[floorName];
        }

        var floorData = new Floor(floorName);
        floors[floorName] = floorData;
        return floorData;
    }

    public bool RemoveFloor(string floorName)
    {
        if (!floors.ContainsKey(floorName))
            return false;

        FloorTransitions.RemoveAll(c => c.floorNameA == floorName || c.floorNameB == floorName);

        return floors.Remove(floorName);
    }

    public Floor Floor(string floorName)
    {
        floors.TryGetValue(floorName, out var floorData);
        return floorData;
    }
}

