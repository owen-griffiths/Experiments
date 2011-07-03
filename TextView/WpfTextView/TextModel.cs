using System;
using System.Collections;
using System.Linq;
using System.Text;
using System.Configuration;
using System.Threading;
using System.Collections.Generic;
using System.ComponentModel;
using TextUtil;

namespace WpfTextView
{
    class TextModel : INotifyPropertyChanged
    {
        private const int TickPeriod = 250;

        private readonly Timer m_timer;

        private int m_maxMemoryMb;
        private int m_usedMemoryMb;
        private string m_gcStatus;

        public int MaxFilesPerZip { get; private set; }

        public int MaxMemoryMb
        {
            get
            {
                return m_maxMemoryMb;
            }

            private set
            {
                if (m_maxMemoryMb != value)
                {
                    m_maxMemoryMb = value;
                    NotifyChange("MaxMemoryMb");
                }
            }
        }

        public int UsedMemoryMb
        {
            get
            {
                return m_usedMemoryMb;
            }

            private set
            {
                if (m_usedMemoryMb != value)
                {
                    m_usedMemoryMb = value;
                    NotifyChange("UsedMemoryMb");
                }
            }
        }

        private void NotifyChange(string propertyName)
        {
            if (PropertyChanged != null)
            {
                PropertyChanged(this, new PropertyChangedEventArgs(propertyName));
            }
        }


        public string GcStatus
        {
            get
            {
                return m_gcStatus;
            }

            private set
            {
                if (m_gcStatus != value)
                {
                    m_gcStatus = value;
                    NotifyChange("GcStatus");
                }
            }
        }

        private static int GetIntConfig(AppSettingsReader settings, string name, int defaultValue)
        {
            try
            {
                return (int)settings.GetValue(name, typeof(int));
            }
            catch
            {
                return defaultValue;
            }
        }

        public TextModel()
        {
            var settings = new AppSettingsReader();
            MaxMemoryMb = GetIntConfig(settings, "MaxMemoryMb", 1024);
            MaxFilesPerZip = GetIntConfig(settings, "MaxFilesPerZip", 25);
            int maxConcurrency = GetIntConfig(settings, "MaxCompressConcurrency", 4);

            m_storage = new CompressedStorage(MaxMemoryMb, maxConcurrency);

            m_timer = new Timer(OnTick, null, TickPeriod, Timeout.Infinite);
            m_storage.OnNewData += OnNewData;
        }

        private void OnNewData(string fileId)
        {
            NotifyChange("CurrentFileStatus");
        }

        public void SetCurrentFile(FileData current)
        {
            if (m_currentFile != null)
            {
                m_currentFile.OnFocusLost();
            }
            m_currentFile = current;
            if (m_currentFile != null)
            {
                m_currentFile.OnFocusGained();
            }

            NotifyChange("CurrentFileStatus");
        }

        public string CurrentFileStatus
        {
            get
            {
                if (m_currentFile != null)
                {
                    return m_currentFile.Status;
                }
                return string.Empty;
            }
        }

        private void OnTick(object sender)
        {
            var counts = new List<string>();
            for (int gen = 0; gen <= GC.MaxGeneration; gen++)
            {
                counts.Add(GC.CollectionCount(gen).ToString());
            }
            GcStatus = string.Format("{0:N0} of {1:N0}[MB] Used in Pool.  GC {2}.  Total Heap {3:N0} [MB]", UsedMemoryMb, MaxMemoryMb, string.Join(",", counts),
                GC.GetTotalMemory(false) / CompressedStorage.MB);

            int fileCount;
            long totalCompressedSize;
            long totalUncompressedSize;

            m_storage.GetTotalStored(out fileCount, out totalCompressedSize, out totalUncompressedSize);

            UsedMemoryMb = (int) (totalCompressedSize/CompressedStorage.MB);
            NotifyChange("CurrentFileStatus");

            m_timer.Change(TickPeriod, Timeout.Infinite);
        }


        public event PropertyChangedEventHandler PropertyChanged;

        internal FileLoader[] LoadFile(string current)
        {
            return FileLoader.Load(current, m_storage);
        }

        private CompressedStorage m_storage;
        private FileData m_currentFile;
    }
}
