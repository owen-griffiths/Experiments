   M S B A M L   `   `   `  ����F  BWpfTextView, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null)clr-namespace:WpfTextViewWpfTextView  5   	       WpfTextView.MainWindow   5   	   \ XPresentationFramework, Version=4.0.0.0, Culture=neutral, PublicKeyToken=31bf3856ad364e35R NWindowsBase, Version=4.0.0.0, Culture=neutral, PublicKeyToken=31bf3856ad364e35W SPresentationCore, Version=4.0.0.0, Culture=neutral, PublicKeyToken=31bf3856ad364e35D 9http://schemas.microsoft.com/winfx/2006/xaml/presentation    5   	   R NSystem.Xaml, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089:x,http://schemas.microsoft.com/winfx/2006/xaml     5   	   ,MyCodeclr-namespace:WpfTextView     5   	   -   5          � Title$  
MainWindow��5   	   $	��659��6   $
��1166��6)    J� 	AllowDrop .  � DataContext 5        WpfTextView.TextModel  5   
   5
      .��5      � .+�5   
   � -   5        $��memoryStatus��5      $��28��5      $
��Left=�6   $��Bottom=�64   $	��110��6O   $��	190,0,0,0q�6[    � Maximum 5      ��  �� Path$ MaxMemoryMb� �� Mode$ OneTime=� � Value 5      �� $ UsedMemoryMb�$ OneWay=� � ToolTip 6>   �� $ MemoryStatusDescription�$ OneWay=�5   [   �� 5   
   -   5        $��
fileStatus��5      $��28��5      $
��Left=�6   $��Bottom=�64   $	��184��6O   ��5      �� $ CurrentFileStatus�5   O   �� 5   
   -   5        $��gcStatus��5      $��28��5      $��Stretch=�6   $��	306,0,0,0q�6;   $��Bottom=�6O   ��5      �� $ GcStatus�$ OneWay=�5   O   �� 5   
   -   5        $��	openFiles��5      $��Stretch=�5      $��Stretch=�63   $
��Auto��6O   $
��Auto��6\   $��0,0,0,34q�6j   5      5      5      