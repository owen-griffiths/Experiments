using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Diagnostics;
using TextUtil;

namespace WpfTextView
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();

            // First argument is the name of the exe
            var args = Environment.GetCommandLineArgs().Skip(1).ToArray();
            LoadFiles(args);
        }

        private void Window_Drop(object sender, DragEventArgs e)
        {
            var paths = (string[]) e.Data.GetData(DataFormats.FileDrop);
            LoadFiles(paths);
        }

        private void LoadFiles(string[] paths)
        {
            if (paths != null)
            {
                var context = (TextModel)DataContext;

                foreach (var current in paths)
                {
                    var loaders = context.LoadFile(current);

                    if (loaders.Length > context.MaxFilesPerZip)
                    {
                        MessageBox.Show(string.Format("{0:N0} files in {1} - Loading first {2:N0}", loaders.Length, current,
                                                      context.MaxFilesPerZip));

                        loaders = loaders.Take(context.MaxFilesPerZip).ToArray();

                        foreach (var ignored in loaders.Skip(context.MaxFilesPerZip))
                        {
                            ignored.Dispose();
                        }
                    }
                    foreach (var currentLoader in loaders.Take(context.MaxFilesPerZip))
                    {
                        TabItem newTab = MakeNewTab(currentLoader);
                        openFiles.Items.Add(newTab);
                    }
                }

                openFiles.SelectedIndex = openFiles.Items.Count - 1;
            }
        }

        private TabItem MakeNewTab(FileLoader currentLoader)
        {
            var newTab = new TabItem {DataContext = new FileData(currentLoader)};

            var headerBinding = new Binding("Name");
            headerBinding.Mode = BindingMode.OneTime;
            newTab.SetBinding(HeaderedContentControl.HeaderProperty, headerBinding);

            newTab.Content = new FileView();
            return newTab;
        }

        private void Window_DragEnter(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                e.Effects = DragDropEffects.Copy;
            }
            else
            {
                e.Effects = DragDropEffects.None;
            }
            e.Handled = true;
        }

        private void Window_KeyUp(object sender, KeyEventArgs e)
        {
        }

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            if (Keyboard.Modifiers == ModifierKeys.Control)
            {
                if (e.Key == Key.W)
                    CloseCurrentTab();
                else if (e.Key == Key.F)
                    ToggleFindDisplayed();
                else if (e.Key == Key.PageUp)
                    MoveCurrentTab(-1);
                else if (e.Key == Key.PageDown)
                    MoveCurrentTab(1);
            }
        }

        private void MoveCurrentTab(int delta)
        {
            int current = openFiles.SelectedIndex;
            int newSelectedIndex = current + delta;

            if ((newSelectedIndex < 0) || (newSelectedIndex >= openFiles.Items.Count))
                return;

            openFiles.SelectedIndex = newSelectedIndex;
        }


        private void CloseCurrentTab()
        {
            var currentTab = (TabItem)openFiles.SelectedItem;
            var disposableContext = currentTab.DataContext as IDisposable;
            if (disposableContext != null)
            {
                disposableContext.Dispose();
            }

            openFiles.Items.Remove(currentTab);
        }

        private void ToggleFindDisplayed()
        {
            var currentTab = (TabItem)openFiles.SelectedItem;
            if (currentTab != null)
            {
                var fileView = (FileView) currentTab.Content;
                fileView.ToggleFindDisplayed();
            }
        }

        private TextModel Model { get { return (TextModel) DataContext; } }

        private void openFiles_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if ((e.AddedItems.Count == 1) && (e.AddedItems[0] is TabItem))
            {
                var tab = (TabItem)e.AddedItems[0];
                var tabData = (FileData) tab.DataContext;

                Model.SetCurrentFile(tabData);
            }
        }

    }
}
