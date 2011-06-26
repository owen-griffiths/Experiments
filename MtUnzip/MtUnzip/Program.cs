using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Xml;
using ICSharpCode.SharpZipLib.BZip2;

namespace MtUnzip
{
    class NodeStats
    {
        public NodeStats(string name)
        {
            m_name = name;
            m_attributeCounts = new SortedDictionary<string, long>();
        }

        public string Name { get { return m_name;  } }

        public long Count { get { return m_count; } }
        public void Update(XmlReader rd)
        {
            Trace.Assert(m_name == rd.Name);
            Trace.Assert(rd.NodeType == XmlNodeType.Element);

            m_count++;

            if (rd.MoveToFirstAttribute())
            {
                do
                {
                    var attrName = rd.Name;
                    if (!m_attributeCounts.ContainsKey(attrName))
                        m_attributeCounts.Add(attrName, 0);

                    m_attributeCounts[attrName] = m_attributeCounts[attrName] + 1;
                } while (rd.MoveToNextAttribute());
            }
        }

        private string m_name;
        private long m_count;
        private SortedDictionary<string, long> m_attributeCounts;
    }

    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length != 2)
            {
                Console.WriteLine("Usage : MtUnZip {NameOfOsm.Bz2} {MtIsEnabled}");
                return;
            }

            if (!File.Exists(args[0]))
            {
                Console.WriteLine("File {0} does not exist", args[0]);
                return;
            }

            bool mtIsEnabled = bool.Parse(args[1]);

            var timer = Stopwatch.StartNew();

            var unzipper = new BZip2InputStream(File.OpenRead(args[0]));

            Stream bufferedReader = unzipper;
            if (mtIsEnabled)
                bufferedReader = new MultiThreadBufferedReader(unzipper);

            var rd = XmlReader.Create(bufferedReader);

            var nodeNameCounts = new List<NodeStats>();

            while (rd.Read())
            {
                if (rd.NodeType == XmlNodeType.Element)
                {
                    var name = rd.Name;

                    var stats = nodeNameCounts.FirstOrDefault(nodeStats => nodeStats.Name == name);
                    if (stats == null)
                    {
                        stats = new NodeStats(name);
                        nodeNameCounts.Add(stats);
                    }

                    stats.Update(rd);
                }
            }

            nodeNameCounts.Sort((lhs, rhs) => lhs.Name.CompareTo(rhs.Name));

            foreach (var stats in nodeNameCounts)
            {
                Console.WriteLine("{0,20} : {1,10:N0}", stats.Name, stats.Count);
            }

            Console.WriteLine("Took {0:N0} [ms]", timer.ElapsedMilliseconds);
            Console.WriteLine("Total Processor time {0:N0} [ms]", Process.GetCurrentProcess().TotalProcessorTime.TotalMilliseconds);
        }
    }
}
