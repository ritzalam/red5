[Setup]
AppName=Red5
AppVerName=Red5 {#version}
AppPublisher=Red5 Project
AppPublisherURL=http://www.osflash.org/red5
DefaultDirName={pf}\Red5
DefaultGroupName=Red5
OutputBaseFilename=setup-red5-{#version}
Compression=lzma
SolidCompression=yes
;Compression=none
WizardSmallImageFile={#build_dir}\images\red5_top.bmp
WizardImageFile={#build_dir}\images\red5_left.bmp

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[CustomMessages]
RegisterService=&Register as service
StartService=&Start service
NoJavaFound=No installation of Java 1.4 / 1.5 was found on your system.
JavaSetup=Java Setup
JavaHome=Java Home
JavaHomeInfo=Enter the path to your Java installation.
InvalidJavaHome=The path you selected is invalid. Please make sure a java.exe exists inside the "bin" directory.

[Tasks]
Name: "service"; Description: "{cm:RegisterService}"
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; Flags: unchecked

[Files]
; Application files
Source: "{#root_dir}\red5.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "{#root_dir}\conf\*.properties"; DestDir: "{app}\conf"; Flags: onlyifdoesntexist recursesubdirs
Source: "{#root_dir}\conf\global.xml"; DestDir: "{app}\conf"; Flags: onlyifdoesntexist recursesubdirs
Source: "{#root_dir}\conf\jetty.xml"; DestDir: "{app}\conf"; Flags: onlyifdoesntexist recursesubdirs; AfterInstall: UpdateConfigFiles('{app}\conf\jetty.xml', '{app}')
Source: "{#root_dir}\conf\red5.xml"; DestDir: "{app}\conf"; Flags: onlyifdoesntexist recursesubdirs; AfterInstall: UpdateConfigFiles('{app}\conf\red5.xml', '{app}')
Source: "{#root_dir}\conf\web.xml"; DestDir: "{app}\conf"; Flags: onlyifdoesntexist recursesubdirs; AfterInstall: UpdateConfigFiles('{app}\conf\web.xml', '{app}')
Source: "{#root_dir}\hosts\*"; DestDir: "{app}\hosts"; Flags: onlyifdoesntexist recursesubdirs
Source: "{#root_dir}\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs
Source: "{#root_dir}\swf\*"; DestDir: "{app}\swf"; Flags: ignoreversion recursesubdirs
Source: "{#root_dir}\webapps\*"; DestDir: "{app}\webapps"; Flags: onlyifdoesntexist recursesubdirs

; Files required for windows service / wrapped start
Source: "{#build_dir}\bin\*.bat"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "{#build_dir}\bin\wrapper.exe"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "{#build_dir}\conf\wrapper.conf"; DestDir: "{app}\conf"; Flags: ignoreversion; AfterInstall: UpdateWrapperConf('{app}\conf\wrapper.conf')
Source: "{#build_dir}\lib\wrapper.dll"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "{#build_dir}\lib\wrapper.jar"; DestDir: "{app}\lib"; Flags: ignoreversion

[Dirs]
Name: "{app}\logs"

[Icons]
Name: "{group}\Red5"; Filename: "{app}\bin\Red5.bat"
Name: "{group}\{cm:UninstallProgram,Red5}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\Red5"; Filename: "{app}\bin\Red5.bat"; Tasks: desktopicon

[Run]
Filename: "{app}\bin\InstallRed5-NT.bat"; Tasks: service; Flags: runhidden;
Filename: "{app}\bin\StartRed5-NT.bat"; Description: "{cm:StartService}"; Tasks: service; Flags: postinstall runhidden;
Filename: "{app}\bin\Red5.bat"; Description: "{cm:LaunchProgram,Red5}"; Tasks: not service; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "{app}\bin\StopRed5-NT.bat"; Tasks: service; Flags: runhidden;
Filename: "{app}\bin\UninstallRed5-NT.bat"; Tasks: service; Flags: runhidden;

[UninstallDelete]
Type: dirifempty; Name: "{app}\logs"

[Code]
var
  JavaHome: String;
  JavaHomePage: TInputDirWizardPage;

function InitializeSetup(): Boolean;
begin
  Result := False;
  // Check Java 1.4 installation
  if not RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\JavaSoft\Java Runtime Environment\1.4', 'JavaHome', JavaHome) then
    JavaHome := '';

  // Check Java 1.5 installation
  if not RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\JavaSoft\Java Runtime Environment\1.5', 'JavaHome', JavaHome) then
    JavaHome := '';
    
  if (JavaHome = '') or (not DirExists(JavaHome)) then begin
    MsgBox(ExpandConstant('{cm:NoJavaFound'), mbCriticalError, MB_OK);
    exit;
  end;

  Result := True;
end;

procedure URLLabelOnClick(Sender: TObject);
var
  Dummy: Integer;
begin
  ShellExec('open', 'http://osflash.org/red5', '', '', SW_SHOWNORMAL, ewNoWait, Dummy);
end;

procedure InitializeWizard();
var
  URLLabel: TNewStaticText;
  CancelButton: TButton;
begin
  // Add link to Red5 homepage on the wizard form
  CancelButton := WizardForm.CancelButton;
  URLLabel := TNewStaticText.Create(WizardForm);
  URLLabel.Left := WizardForm.ClientWidth - CancelButton.Left - CancelButton.Width;
  URLLabel.Top := CancelButton.Top;
  URLLabel.Caption := 'http://osflash.org/red5';
  URLLabel.Font.Style := URLLabel.Font.Style + [fsUnderLine];
  URLLabel.Font.Color := clBlue;
  URLLabel.Cursor := crHand;
  URLLabel.OnClick := @URLLabelOnClick;
  URLLabel.Parent := WizardForm;
  
  JavaHomePage := CreateInputDirPage(wpSelectTasks,
    ExpandConstant('{cm:JavaSetup}'),
    ExpandConstant('{cm:JavaHome}'),
    ExpandConstant('{cm:JavaHomeInfo}'),
    False,
    '');
  JavaHomePage.Add('');
  JavaHomePage.Values[0] := JavaHome;
end;

function IsValidJavaHome(Path: String): Boolean;
begin
  Path := AddBackslash(Path);
  Result := FileExists(Path + 'bin\java.exe');
end;

function NextButtonClick(CurPage: Integer): Boolean;
begin
  Result := True;
  if (CurPage = JavaHomePage.ID) then begin
    if not IsValidJavaHome(JavaHomePage.Values[0]) then begin
      MsgBox(ExpandConstant('{cm:InvalidJavaHome}'), mbError, MB_OK);
      Result := False;
    end;
  end;
end;

function UpdateReadyMemo(Space, NewLine, MemoUserInfoInfo, MemoDirInfo, MemoTypeInfo, MemoComponentsInfo, MemoGroupInfo, MemoTasksInfo: String): String;
begin
  Result := MemoDirInfo + NewLine + NewLine +
            MemoGroupInfo + NewLine + NewLine +
            MemoTasksInfo + NewLine + NewLine +
            ExpandConstant('{cm:JavaHome}') + NewLine +
            Space + AddBackslash(JavaHomePage.Values[0]);
end;

procedure UpdateConfigFiles(Filename: String; Root: String);
var
  Lines: TArrayOfString;
  i: Integer;
  ConfigRoot: String;
begin
  Filename := ExpandConstant(Filename);
  Root := ExpandConstant(Root);
  ConfigRoot := Root + '/conf';
  // Update path to included configuration files
  if LoadStringsFromFile(Filename, Lines) then begin
    for i := 0 to GetArrayLength(Lines)-1 do begin
      if Pos(Root, Lines[i]) > 0 then
        // Already changed this line...
        continue;

      if Pos('./conf/', Lines[i]) > 0 then begin
        StringChange(Lines[i], './conf/', ConfigRoot + '/');
      end else if Pos('/conf/', Lines[i]) > 0 then begin
        StringChange(Lines[i], '/conf/', ConfigRoot + '/');
      end else if Pos('conf/', Lines[i]) > 0 then begin
        StringChange(Lines[i], 'conf/', ConfigRoot + '/');
      end;
      
      if Pos('</value></constructor-arg>', Lines[i]) > 0 then begin
        StringChange(Lines[i], '</constructor-arg>', '</constructor-arg><constructor-arg><value>..\hosts</value></constructor-arg>');
      end;

      if Pos('./webapps', Lines[i]) > 0 then begin
        StringChange(Lines[i], './webapps', Root + '/webapps');
      end;
    end;
    SaveStringsToFile(Filename, Lines, False);
  end;
end;

procedure UpdateWrapperConf(Filename: String);
var
  Lines: TArrayOfString;
  i: Integer;
  Path: String;
begin
  Filename := ExpandConstant(Filename);
  Path := AddBackslash(JavaHomePage.Values[0]);
  if LoadStringsFromFile(Filename, Lines) then begin
    for i := 0 to GetArrayLength(Lines)-1 do begin
      if Pos(JavaHome, Lines[i]) > 0 then
        // Already changed this line...
        continue;

      if Pos('wrapper.java.command=', Lines[i]) > 0 then begin
        Lines[i] := Format('wrapper.java.command=%sbin\java.exe', [Path]);
      end;
    end;
    SaveStringsToFile(Filename, Lines, False);
  end;
end;

