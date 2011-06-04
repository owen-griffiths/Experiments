using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

// Problem "Safest Place" from 2011 Facebook Hacker Cup final 
// http://www.facebook.com/notes/facebook-hacker-cup/facebook-hacker-cup-finals/208549245827651

//While en route to the 295th annual Galactic Dance Party on Risa, you find yourself unceremoniously yanked 
//out of hyperspace and, according to your sensors, surrounded by N space bombs.  
//Apparently caught in a trap laid out by some dastardly and unknown enemy, and unable 
//to return to hyperspace, you must find the safest place in the vicinity to weather the 
//detonation of all the space bombs.  
//Your unseen opponent has constructed a cube-shaped space anomaly that you are unable 
//to leave, so your options are limited to points within that cube.

//Before the bombs explode (all simultaneously), you have just enough time to travel to any integer point 
//in the cube [0, 0, 0]-[1000, 1000, 1000], both inclusive.  You must find the point with the maximum 
//distance to the nearest bomb, which your captain's intuition tells you will be the safest point.

namespace SpaceCube
{
    struct Point3d
    {
        public int X { get; set; }
        public int Y { get; set; }
        public int Z { get; set; }
        
        public Point3d(int x, int y, int z) : this()
        {
            X = x;
            Y = y;
            Z = z;
        }

        public int DistToSqrd(Point3d other)
        {
            int dx = X - other.X;
            int dy = Y - other.Y;
            int dz = Z - other.Z;

            return dx*dx + dy*dy + dz*dz;
        }

        public override string ToString()
        {
            return string.Format("{0},{1},{2}", X, Y, Z);
        }

        public override bool Equals(object obj)
        {
            var rhs = (Point3d) obj;
            return (X == rhs.X) && (Y == rhs.Y) && (Z == rhs.Z);
        }

        public override int GetHashCode()
        {
            return X.GetHashCode() * 13 + Y.GetHashCode() * 169 + Z.GetHashCode();
        }
    }

    struct Range
    {
        public int Begin { get; private set; }
        public int End { get; private set; }        // Exclusive

        public Range(int begin, int end) : this()
        {
            Begin = begin;
            End = end;
        }
    }

    class Program
    {
        // PRE  : exclusions sorted by Begin
        static IEnumerable<int> ValuesLeft(List<Range> exclusions)
        {
            int next = 0;
            foreach (var e in exclusions)
            {
                if (e.End > next)
                {
                    for (int result = next; result < e.Begin; result++)
                        yield return result;

                    next = Math.Max(next, e.End);
                }
            }

            for (int result = next; result < CubeSize; result++)
                yield return result;
        }

        static List<Range> ProjectBombs(int x, int y, Point3d[] bombsByZ, int radSqr)
        {
            var result = new List<Range>();

            foreach (var b in bombsByZ)
            {
                int dx = b.X - x;
                int dy = b.Y - y;

                int xYDistSqr = dx*dx + dy*dy;

                if (radSqr >= xYDistSqr)
                {
                    int maxDz = (int)Math.Sqrt(radSqr - xYDistSqr);
                    var newRange = new Range(b.Z - maxDz, b.Z + maxDz + 1);

                    // The intersection of a bomb is symetric around its Z
                    // Also, each bomb has Z >= all previous ones
                    // Thus, if the Begin of the range is less than a previous Begin, the new range totally superceeds the previous range, 
                    //  and the previous range can be removed
                    for (int iExisting = result.Count - 1; iExisting >= 0; iExisting--)
                    {
                        if (newRange.Begin <= result[iExisting].Begin)
                        {
                            result.RemoveAt(iExisting);
                        }
                        else
                        {
                            // result is build up sorted by Begin
                            // Thus, if Begin at iExisting is too small, there is no point continuing back through the list
                            break;
                        }
                    }

                    result.Add(newRange);
                }
            }

            return result;
        }

        static void FindSafest(List<Point3d> bombs)
        {
            var bombsByZ = bombs.OrderBy(b => b.Z).ToArray();

            int bestRad = 0;
            var bestPlace = new Point3d();

            // Basic algorithm - each x = x1, y = y1 defines a line of CubeSize points for each Z
            // Project each bomb with current best radius onto this line to find
            //  section where the sphere around each bomb intersects
            // Then, only need to check Z values not in these intersections
            for (int x = 0; x < CubeSize; x++)
            {
                for (int y = 0; y < CubeSize; y++)
                {
                    var exclusions = ProjectBombs(x, y, bombsByZ, bestRad);

                    int oldRad = bestRad;
                    foreach (int z in ValuesLeft(exclusions))
                    {
                        var p = new Point3d(x, y, z);

                        int candidate = FindMinRadius(p, bombs);
                        if (candidate > bestRad)
                        {
                            bestRad = candidate;
                            bestPlace = p;
                        }
                    }

                    if (bestRad > oldRad)
                    {
                        Console.WriteLine("{0,20} : Cursor : {1} : Found new radius of {2}", TimeStamp(), bestPlace, bestRad);
                    }
                }
            }

            Console.WriteLine("Total Time = {0}", s_timer.Elapsed);
            Console.WriteLine("Safest radius {0} @ {1}", bestRad, bestPlace);
        }

        private static int FindMinRadius(Point3d p, List<Point3d> bombs)
        {
            var result = bombs.Select(b => b.DistToSqrd(p)).Min();
            return result;
        }

        private const int CubeSize = 1001;
        private static readonly Stopwatch s_timer = Stopwatch.StartNew();

        static string TimeStamp()
        {
            return (DateTime.Today + s_timer.Elapsed).ToString("mm:ss");
        }

        static void Main()
        {
            var bombs = new List<Point3d>();
            var rand = new Random();

            while (bombs.Count < 200)
            {
                int x = rand.Next(CubeSize);
                int y = rand.Next(CubeSize);
                int z = rand.Next(CubeSize);

                var next = new Point3d(x, y, z);

                if (!bombs.Contains(next))
                {
                    bombs.Add(next);
                }
            }
            // Add in Corner bombs, as otherwise the corners tend to be the safest spots, which isn't very interesting
            bombs.Add(new Point3d(0, 0, 0));
            bombs.Add(new Point3d(0, 0, 1000));
            bombs.Add(new Point3d(0, 1000, 0));
            bombs.Add(new Point3d(0, 1000, 1000));
            bombs.Add(new Point3d(1000, 0, 0));
            bombs.Add(new Point3d(1000, 0, 1000));
            bombs.Add(new Point3d(1000, 1000, 0));
            bombs.Add(new Point3d(1000, 1000, 1000));

            // Add the center of each face
            bombs.Add(new Point3d(0, 500, 500));
            bombs.Add(new Point3d(1000, 500, 500));

            bombs.Add(new Point3d(500, 0, 500));
            bombs.Add(new Point3d(500, 1000, 500));

            bombs.Add(new Point3d(500, 500, 0));
            bombs.Add(new Point3d(500, 500, 1000));

            Console.WriteLine("{0} bombs - eg {1}", bombs.Count, bombs.First());
            Console.WriteLine();

            FindSafest(bombs);
        }
    }
}
