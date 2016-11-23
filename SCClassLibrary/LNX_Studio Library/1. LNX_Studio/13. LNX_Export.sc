// EXPORT STEMS //
/*
a.quickLoad;
a.exporter.exportStems;

a.loadDemoSong(true);
(
g = [LNX_ExportGroup("drums"),
    LNX_ExportGroup("bass"),
    LNX_ExportGroup("strings"),
    LNX_ExportGroup("synth"),
    LNX_ExportGroup("piano")];
g[0].addInst(a.insts.mixerInstruments[0]);
g[0].addInst(a.insts.mixerInstruments[1]);
g[0].addInst(a.insts.mixerInstruments[2]);
g[1].addInst(a.insts.mixerInstruments[3]);
g[1].addInst(a.insts.mixerInstruments[4]);
g[2].addInst(a.insts.mixerInstruments[5]);
g[3].addInst(a.insts.mixerInstruments[6]);
g[3].addInst(a.insts.mixerInstruments[7]);
g[4].addInst(a.insts.mixerInstruments[8]);
a.exporter.exportStems(groups: g);
)

g[0].removeInst(a.insts.mixerInstruments[2]);
g[0].insts;

a.exporter.skipExport;
a.exporter.stopExport;
*/
LNX_Export {

    var <studio, <isExporting=false,
        <continueExportingThis=true, <continueExportingAll=true;

    *new {|studio| ^super.new.init(studio) }

    init {|argStudio|
        studio=argStudio
    }

    stopExport {
        continueExportingThis = false;
        continueExportingAll = false;
    }

    skipExport {
        continueExportingThis = false;
        continueExportingAll = true;
    }

    exportStems {|extraTime=3, groups=nil|
        var i = 0,
            n = 0,
            s = studio;

        if (groups.isNil) {
            n = s.insts.mixerInstruments.size;
            groups = Array.fill(n, {|i|
                var g, inst;
                inst = s.insts.mixerInstruments[i];
                g = LNX_ExportGroup(inst.id + "-" + inst.name);
                g.name.postln;
                g.addInst(inst);
            });
        };

        n = groups.size;

        Dialog.savePanel({|path|
            path.mkdir;
            isExporting = true;
            continueExportingThis = true;
            continueExportingAll = true;
            { this.exportNext(path, groups, i, n, extraTime); }.fork;
        });
    }

    exportNext {|path, groups, i, n, extraTime|
        var s, group, path1,
            pauseTime, endTime, t;

        groups.postln;

        s = studio;
        group = groups[i];

        // remove instruments not in group
        s.insts.mixerInstruments.do {|inst|
            if (group.hasInst(inst).not) {
                { s.deleteInst(inst.id) }.defer(0);
            };
        };

        pauseTime = MVC_Automation.absDuration;
        endTime = pauseTime + extraTime;
        t = 0;

        path1 = s.server.prepareForRecord(
            path +/+
            group.name ++
            "." ++ (s.server.recHeaderFormat)
        );

        0.2.wait;

        s.server.record;

        0.5.wait;

        { s.play(LNX_Protocols.latency, s.beat); }.defer(0);

        while ({continueExportingThis and: (t < pauseTime)}, {
            t = t+s.absTime;
            s.absTime.wait;
        });

        { s.pause; }.defer(0);

        while ({continueExportingThis and: (t < endTime)}, {
            t = t+s.absTime;
            s.absTime.wait;
        });

        s.server.stopRecording;

        i = i+1;

        { s.quickLoad }.defer(0);
        
        if (continueExportingAll and: (i < n)) {
            continueExportingThis = true;
            0.1.wait;
            while ({ s.isLoading }) { 0.5.wait; };
            3.wait;
            this.exportNext(path, groups, i, n, extraTime);
        } {
            isExporting = false;
            "Export complete".postln;
        }
    }
}

LNX_ExportGroup {

    var <insts, <>name;

    *new {|name| ^super.new.init(name) }

    init {|argName="export"|
        insts = List.new;
        name = argName;
    }

    addInst {|inst|
        insts.add(inst.id);
    }

    removeInst {|inst|
        if (this.hasInst(inst)) {
            insts.removeAt(insts.indexOf(inst.id));
        };
    }

    hasInst {|inst|
        insts.do {|id|
            if (inst.id == id) {
                ^true;
            }
        }
        ^false;
    }

}