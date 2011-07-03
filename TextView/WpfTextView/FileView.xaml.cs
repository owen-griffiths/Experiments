using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Automation.Peers;
using System.Windows.Automation.Provider;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Windows.Threading;
using TextUtil;

namespace WpfTextView
{
    /// <summary>
    /// Interaction logic for FileView.xaml
    /// </summary>
    public partial class FileView : UserControl
    {
        public FileView()
        {
            InitializeComponent();
        }

        public FileData Context { get { return (FileData) DataContext; } }

        private void findButton_Click(object sender, RoutedEventArgs e)
        {
            if ((FindRow.Height.Value > 0) && (findText.Text != string.Empty))
                Context.Find(findText.Text);
        }

        private void stopButton_Click(object sender, RoutedEventArgs e)
        {
            Context.StopFind();
        }

        internal void ToggleFindDisplayed()
        {
            if (m_savedFindHeight.Value > 0)
            {
                FindRow.Height = m_savedFindHeight;
                findText.Focus();
                m_savedFindHeight = new GridLength(-1);
            }
            else
            {
                m_savedFindHeight = FindRow.Height;
                FindRow.Height = new GridLength(0);
            }
        }

        private void ScrollTo(CompressedFileSearcher.MatchResult match)
        {
            int lineNumber;

            if (int.TryParse(match.LineNumber, NumberStyles.AllowThousands, CultureInfo.InvariantCulture, out lineNumber))
            {
                var svAutomation = (ListBoxAutomationPeer)ScrollViewerAutomationPeer.CreatePeerForElement(textView);
                var scrollInterface = (IScrollProvider)svAutomation.GetPattern(PatternInterface.Scroll);

                double totalCount = Context.Contents.Count;
                double linePercent = 100.0 * lineNumber / totalCount;
                try
                {
                    scrollInterface.SetScrollPercent(System.Windows.Automation.ScrollPatternIdentifiers.NoScroll, linePercent);
                    Trace.WriteLine(string.Format("Selected line {0}, {1}% of {2}", lineNumber, linePercent,
                                                  totalCount));
                }
                catch (Exception ex)
                {
                    Trace.WriteLine(ex.Message);
                }
            }
        }

        private void listView1_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            var p = e.GetPosition(listView1);
            var htr = VisualTreeHelper.HitTest(listView1, p);
            
            if (htr == null) return;

            for (var cursor = htr.VisualHit; cursor != null; cursor = VisualTreeHelper.GetParent(cursor))
            {
                var item = cursor as ListBoxItem;
                if (item != null)
                {
                    ScrollTo((CompressedFileSearcher.MatchResult) item.Content);
                    return;
                }
            }
        }


        private void listView1_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (e.AddedItems.Count == 1)
            {
                var selected = (CompressedFileSearcher.MatchResult) e.AddedItems[0];
                ScrollTo(selected);
            }
        }

        private GridLength m_savedFindHeight = new GridLength(150);

        private void UserControl_KeyDown(object sender, KeyEventArgs e)
        {
            if (Keyboard.Modifiers == ModifierKeys.Control)
            {
                if (e.Key == Key.N)
                {
                    var contents = (StoredFile) Context.Contents;
                    contents.ShowLineNumbers = !contents.ShowLineNumbers;
                    contents.OnUpdated();
                }
                else if (e.Key == Key.C)
                {
                    var toCopy = new StringBuilder();

                    foreach (var item in textView.SelectedItems.Cast<StoredFile.Line>())
                        toCopy.AppendLine(item.RawLine);

                    Clipboard.SetText(toCopy.ToString());
                }
            }
        }
    }

    public class StoredFile : IList, INotifyCollectionChanged
    {
        // Having problems with selection in files with multiple lines the same
        // The WPF ListBox seems to use string.Equals to determine which line is selected, and doesn't work for files with multiple lines the same
        // Thus, I wrap each line in a separate object.  This will use reference equality, so won't mismatch with repeated lines
        internal class Line
        {
            public Line(string l, long number, StoredFile parent)
            {
                m_line = l;
                m_lineNumber = number;
                m_parent = parent;
            }

            public override string ToString()
            {
                if (m_parent.ShowLineNumbers)
                {
                    int padSize = m_parent.Count.ToString("N0").Length;
                    string formattedLineNumber = m_lineNumber.ToString("N0").PadLeft(padSize);

                    return string.Format("[{0}]  {1}", formattedLineNumber, m_line);
                    
                }
                
                return m_line;
            }

            public string RawLine { get { return m_line;  } }

            private string m_line;
            private long m_lineNumber;
            private StoredFile m_parent;
        }

        public StoredFile(CompressedStorage storage, string id)
        {
            m_storage = storage;
            m_id = id;
            m_asyncOp = AsyncOperationManager.CreateOperation(null);
        }

        public void RemoveAt(int index)
        {
            throw new NotImplementedException();
        }

        public object this[int index]
        {
            get
            {
                // index 0 maps to line number 1
                return new Line(m_storage.GetLine(m_id, index+1), index+1, this);
            }
            set
            {
                throw new NotImplementedException();
            }
        }

        public void Clear()
        {
            throw new NotImplementedException();
        }

        public int Count
        {
            get
            {
                long lineCount;
                int spanCount;

                m_storage.FileStatus(m_id, out lineCount, out spanCount);

                return (int) lineCount;
            }
        }

        public bool ShowLineNumbers { get; set; }

        public bool IsReadOnly
        {
            get { return true; }
        }

        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator()
        {
            return new ListEnumerator(this);
        }

        // Can be called from any thread
        public void OnUpdated()
        {
            m_asyncOp.Post(RaiseCollectionChanged, null);
        }

        // Will now be on the dispatcher thread
        private void RaiseCollectionChanged(object param)
        {
            if (CollectionChanged != null)
            {
                CollectionChanged(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
            }
        }

        private CompressedStorage m_storage;
        private string m_id;
        private AsyncOperation m_asyncOp;

        public event NotifyCollectionChangedEventHandler CollectionChanged;

        public int Add(object value)
        {
            throw new NotImplementedException();
        }

        public bool Contains(object value)
        {
            return false;
        }

        public int IndexOf(object value)
        {
            return -1;
        }

        public void Insert(int index, object value)
        {
            throw new NotImplementedException();
        }

        public bool IsFixedSize
        {
            get
            {
                return true;
            }
        }

        public void Remove(object value)
        {
            throw new NotImplementedException();
        }

        public void CopyTo(Array array, int index)
        {
            throw new NotImplementedException();
        }

        public bool IsSynchronized
        {
            get { return false; }
        }

        public object SyncRoot
        {
            get { return this; }
        }
    }

    public class FileData : INotifyPropertyChanged, IDisposable
    {
        public FileData(FileLoader loader)
        {
            m_loader = loader;
            m_loader.Storage.OnNewData += OnUpdatedContents;
            m_contents = new StoredFile(m_loader.Storage, m_loader.Id);

            m_searcher = m_loader.Storage.MakeSearcher(m_loader.Id);
            FindResults = new ObservableCollection<CompressedFileSearcher.MatchResult>();

            m_findResultsTimer = new DispatcherTimer {Interval = TimeSpan.FromMilliseconds(250)};
            m_findResultsTimer.Tick += CheckForNewResults;
            m_findResultsTimer.Start();

            CanStartFind = true;
        }

        private void CheckForNewResults(object o, EventArgs a)
        {
            if (FindIsInProgress)
            {
                int findProgressPercent;
                if (m_searcher.GetNewResults(FindResults, out findProgressPercent))
                {
                    StopFind();
                }
                FindPercent = findProgressPercent;
            }
        }

        public string Name { get { return m_loader.Title; } }

        public IList Contents { get { return m_contents; } }

        public ObservableCollection<CompressedFileSearcher.MatchResult> FindResults { get; private set; }

        public bool CanStartFind { get; private set; }

        public bool FindIsInProgress { get; private set; }

        public void OnUpdatedContents(string fileId)
        {
            if (fileId == m_loader.Id)
            {
                m_contents.OnUpdated();
                NotifyProperty("Contents");
            }
        }

        public event PropertyChangedEventHandler PropertyChanged;

        public void Dispose()
        {
            m_loader.Storage.OnNewData -= OnUpdatedContents;
            m_loader.Dispose();
            m_loader.Storage.Unload(m_loader.Id);
        }

        public void OnFocusLost()
        {
            m_loader.Pause();
        }

        public void OnFocusGained()
        {
            m_loader.Start();
        }

        private FileLoader m_loader;
        private StoredFile m_contents;
        private CompressedFileSearcher m_searcher;
        private DispatcherTimer m_findResultsTimer;
        private int m_findPercent;

        public string Status
        {
            get
            {
                long lineCount;
                int spanCount;
                m_loader.Storage.FileStatus(m_loader.Id, out lineCount, out spanCount);

                if (m_loader.IsFinished)
                    return string.Format("Loaded {0:N0} lines", lineCount);

                return string.Format("Loading... {0:N0} lines", lineCount);
            }
        }

        public void NotifyProperty(string name)
        {
            if (PropertyChanged != null)
            {
                PropertyChanged(this, new PropertyChangedEventArgs(name));
            }
        }

        public int FindPercent 
        {
            get { return m_findPercent; } 
            set
            {
                if (m_findPercent != value)
                {
                    m_findPercent = value;
                    NotifyProperty("FindPercent");
                }
            }
        }

        internal void Find(string toFind)
        {
            CanStartFind = false;
            FindIsInProgress = true;
            FindPercent = 0;

            NotifyProperty("CanStartFind");
            NotifyProperty("FindIsInProgress");

            FindResults.Clear();
            m_searcher.StartFind(toFind, 1000);
        }

        internal void StopFind()
        {
            m_searcher.StopFind();

            CanStartFind = true;
            FindIsInProgress = false;

            NotifyProperty("CanStartFind");
            NotifyProperty("FindIsInProgress");
        }
    }
}
