using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using ICSharpCode.SharpZipLib.BZip2;
using ICSharpCode.SharpZipLib.Zip;

namespace TextUtil
{
    public class FileLoader : IDisposable
    {
        // Can return an array if path is a zip with multiple files
        public static FileLoader[] Load(string path, CompressedStorage storage)
        {
            var extension = Path.GetExtension(path).ToLower();

            if (extension == ".bz2")
            {
                return LoadBzip(path, storage);
            }
            if (extension == ".zip")
            {
                return LoadZip(path, storage);
            }

            return LoadStandardFile(path, storage);
        }

        public CompressedStorage Storage { get { return m_storage; } }

        public void Start()
        {
            m_pauseLoading = false;
            if (m_readThread == null)
            {
                m_readThread = new Thread(DoLoad) {IsBackground = true};
                m_readThread.Start();
            }
        }

        public void Pause()
        {
            m_pauseLoading = true;
        }

        public bool IsFinished
        {
            get
            {
                return m_isFullyLoaded;
            }
        }

        public string Id
        {
            get { return m_fileId; }
        }

        public string Title { get; private set; }

        public void Dispose()
        {
            m_quit = true;
            m_readThread.Join();
        }

        private void DoLoad()
        {
            var block = new List<string>();
            var blockLength = 0;

            while (!m_quit)
            {
                if (m_pauseLoading || m_storage.IsFull) 
                {
                    Thread.Sleep(TimeSpan.FromSeconds(0.1));
                }
                else
                {
                    var nextLine = m_reader.ReadLine();

                    if (nextLine == null)
                    {
                        m_storage.AddSpan(m_fileId, block);
                        break;
                    }

                    block.Add(nextLine);
                    blockLength += nextLine.Length;

                    if (blockLength > 1000*1000)
                    {
                        m_storage.AddSpan(m_fileId, block);

                        block.Clear();
                        blockLength = 0;
                    }
                }
            }

            m_reader.Close();
            m_storage.WaitForPendingSpans();
            m_isFullyLoaded = true;
        }

        private bool m_pauseLoading = true;
        private bool m_quit;
        private readonly StreamReader m_reader;
        private Thread m_readThread;
        private readonly CompressedStorage m_storage;
        private readonly string m_fileId;
        private bool m_isFullyLoaded;

        private FileLoader(StreamReader rd, CompressedStorage storage, string fileId, string title)
        {
            m_reader = rd;
            m_storage = storage;
            m_fileId = fileId;
            Title = title;
        }

        private static FileLoader[] LoadStandardFile(string path, CompressedStorage storage)
        {
            var rd = File.OpenText(path);
            var fileId = storage.AddFile(path);
            var title = Path.GetFileName(path);

            return new[] {new FileLoader(rd, storage, fileId, title)};
        }

        private static FileLoader[] LoadBzip(string path, CompressedStorage storage)
        {
            var uncompressor = new BZip2InputStream(File.OpenRead(path));
            var rd = new StreamReader(uncompressor);
            var fileId = storage.AddFile(path);
            var title = Path.GetFileName(path);

            return new[] { new FileLoader(rd, storage, fileId, title) };
        }

        private static FileLoader[] LoadZip(string path, CompressedStorage storage)
        {
            var zip = new ZipFile(path);
            var result = new List<FileLoader>();
            for (int i = 0; i < zip.Count; i++)
            {
                var entry = zip[i];
                if (entry.IsFile)
                {
                    var uncompressor = zip.GetInputStream(i);
                    var rd = new StreamReader(uncompressor);
                    var fileId = storage.AddFile(path);
                    var title = string.Format("{0}:{1}", Path.GetFileNameWithoutExtension(path), zip[i].Name);

                    result.Add(new FileLoader(rd, storage, fileId, title));
                }
            }

            return result.ToArray();
        }
    }
}
