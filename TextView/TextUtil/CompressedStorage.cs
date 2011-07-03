using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;
using System.Collections.ObjectModel;

namespace TextUtil
{
    public class CompressedStorage
    {
        public const int MB = 1024*1024;
        public CompressedStorage(long targetCapacityMb, int maxCompressedConcurrency)
        {
            TargetCapacityMb = targetCapacityMb;
            m_maxConcurrent = maxCompressedConcurrency;
        }

        public string AddFile(string name)
        {
            // Append GUID to ensure total uniqueness
            var id = string.Format("{0}_{1}", name, Guid.NewGuid());

            lock (m_lock)
            {
                m_fileData.Add(id, new List<Span>());
            }

            return id;
        }

        private void ProcessCompressedSpan(KeyValuePair<string, Span> compressResult)
        {
            var fileId = compressResult.Key;

            // All this work should be light, as it won't be done in parallel with other tasks
            lock (m_lock)
            {
                List<Span> spans;
                // If this fails, there must have been an unload of this file
                if (m_fileData.TryGetValue(fileId, out spans))
                {
                    long spanStart = 1;
                    if (spans.Count > 0)
                    {
                        spanStart += spans.Last().LastLine;
                    }

                    var newSpan = compressResult.Value;
                    newSpan.FirstLine = spanStart;

                    spans.Add(newSpan);
                    UpdateIsFull();
                }
            }

            NotifyNewData(fileId);
        }

        // PRE  : Not thread safe - must not be called again until this call has exited
        // Must be called with sequential spans from the file
        public void AddSpan(string fileId, List<string> span)
        {
            if (span.Count == 0)
                return;

            // As I am creating a separate task, I will access the span data at an indeterminate time
            //  so must make a local copy.  Thus, I will not access the passed span object after this function terminates
            var mySpan = span.ToArray();

            lock (m_compressionQueue)
            {
                if (m_compressionQueue.Count >= m_maxConcurrent)
                {
                    var waitFor = m_compressionQueue.Dequeue();
                    ProcessCompressedSpan(waitFor.Result);
                }

                var newTask = Task.Factory.StartNew(() => CompressSpan(fileId, mySpan));
                m_compressionQueue.Enqueue(newTask);
            }
        }

        public void WaitForPendingSpans()
        {
            Task<KeyValuePair<string, Span>>[] queueContents;
            lock (m_compressionQueue)
            {
                queueContents = m_compressionQueue.ToArray();
                m_compressionQueue.Clear();
            }

            foreach (var task in queueContents)
            {
                ProcessCompressedSpan(task.Result);
            }
        }

        public void Unload(string fileId)
        {
            lock (m_lock)
            {
                if (m_fileData.ContainsKey(fileId))
                {
                    m_fileData.Remove(fileId);
                    UpdateIsFull();
                }
            }
            GC.Collect();
        }

        public bool FileStatus(string fileId, out long lineCount, out int spanCount)
        {
            lock (m_lock)
            {
                List<Span> spans;

                lineCount = 0;
                spanCount = 0;

                if (m_fileData.TryGetValue(fileId, out spans))
                {
                    spanCount = spans.Count;
                    if (spanCount > 0)
                    {
                        lineCount = spans.Last().LastLine;
                    }
                    return true;
                }

                return false;
            }
        }

        public bool IsFull { get; private set; }

        public long TargetCapacityMb { get; private set; }

        public void UpdateIsFull()
        {
            lock (m_lock)
            {
                long totalCompressedSize = m_fileData.Values.Sum(spanList => spanList.Sum(span => span.CompressedSize));

                IsFull = (totalCompressedSize/MB) >= TargetCapacityMb;
            }
        }

        public void GetTotalStored(out int fileCount, out long totalCompressedSize, out long totalUncompressedSize)
        {
            lock (m_lock)
            {
                fileCount = m_fileData.Count;

                totalCompressedSize = 0;
                totalUncompressedSize = 0;
                foreach (var spans in m_fileData.Values)
                {
                    totalCompressedSize += spans.Sum(s => s.CompressedSize);
                    totalUncompressedSize += spans.Sum(s => s.CharCount);
                }
            }
        }


        public override string ToString()
        {
            long totalCompressedSize;
            long totalChars;
            int fileCount;

            GetTotalStored(out fileCount, out totalCompressedSize, out totalChars);

            return string.Format("{0:N0}[b] for {1} files ({2:N0}[b] compressed)", totalChars, fileCount, totalCompressedSize);
        }

        public string GetLine(string id, int index)
        {
            string result = string.Empty;
            lock (m_lock)
            {
                List<Span> spans;
                if (m_fileData.TryGetValue(id, out spans))
                {
                    foreach (var s in spans)
                    {
                        if (s.GetLine(index, ref result))
                        {
                            break;
                        }
                    }
                }
            }
            return result;
        }

        // MatchResults will be added to matches in increasing line order
        public CompressedFileSearcher MakeSearcher(string fileId)
        {
            return new CompressedFileSearcher(this, fileId);
        }

        internal readonly Span[] NoSpans = new Span[0];

        internal Span[] GetCurrentSpans(string fileId)
        {
            lock (m_lock)
            {
                List<Span> spans;
                if (m_fileData.TryGetValue(fileId, out spans))
                {
                    return spans.ToArray();
                }
            }

            return NoSpans;
        }

        private KeyValuePair<string, Span> CompressSpan(string fileId, string[] span)
        {
            long charCount = span.Sum(s => s.Length);

            var compressed = CompressUtil.Compress(span);
            var resultSpan = new Span(compressed, -1, span.Length, charCount);

            return new KeyValuePair<string, Span>(fileId, resultSpan);
        }

        private void NotifyNewData(string fileId)
        {
            if (OnNewData != null)
            {
                OnNewData(fileId);
            }
        }

        public delegate void OnNewDataHandler(string fileId);

        public event OnNewDataHandler OnNewData;

        internal class Span
        {
            public long FirstLine { get; set; }
            public long LastLine { get { return FirstLine + LineCount - 1; } }
            public long LineCount { get; private set; }
            public long CharCount { get; private set; }

            private byte[] m_compressedData;
            private WeakReference m_uncompressed = new WeakReference(null);
            private object m_lock = new object();

            public Span(byte[] compressedData, long firstLine, long lineCount, long charCount)
            {
                m_compressedData = compressedData;
                FirstLine = firstLine;
                LineCount = lineCount;
                CharCount = charCount;
            }

            public string[] GetContents()
            {
                lock (m_lock)
                {
                    var result = (string[])m_uncompressed.Target;

                    if (result == null)
                    {
                        result = CompressUtil.Uncompress<string[]>(m_compressedData);
                        m_uncompressed.Target = result;
                    }

                    return result;
                }
            }

            public int CompressedSize { get { return m_compressedData.Length; } }

            internal bool GetLine(int index, ref string result)
            {
                if ((index >= FirstLine) && (index <= LastLine))
                {
                    long offset = index - FirstLine;
                    result = GetContents()[offset];
                    return true;
                }

                return false;
            }
        };

        private readonly Dictionary<string, List<Span>> m_fileData = new Dictionary<string, List<Span>>();
        private readonly object m_lock = new object();
        private readonly Queue<Task<KeyValuePair<string, Span>>> m_compressionQueue = new Queue<Task<KeyValuePair<string, Span>>>();
        private readonly int m_maxConcurrent = 4;
    }
}
