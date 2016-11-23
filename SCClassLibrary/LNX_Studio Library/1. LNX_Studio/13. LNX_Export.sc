LNX_Export {

    var <studio;

    *new {|studio| ^super.new.init(studio) }

    init {|argStudio| studio=argStudio }

    // EXPORT STEMS //
    /*
    a.quickLoad;
    a.loadDemoSong(true);
    a.exporter.exportStems;
    */
    exportStems {|extraTime=3|
        var i = 0,
            n = studio.insts.mixerInstruments.size;

        Dialog.savePanel({ arg path;

            path.mkdir;

            { this.exportNext(path, i, n, extraTime); }.fork;
            
        });

    }

    exportNext {|path, i, n, extraTime|
        var s, inst, path1,
            pauseTime, endTime, t;
        s = studio;
        inst = s.insts.mixerInstruments[i];
        inst.postln;
        s.insts.mixerInstruments.do {|inst2|
            if (inst2.id != inst.id) {
                { s.deleteInst(inst2.id) }.defer(0);
            };
        };

        pauseTime = MVC_Automation.absDuration;
        endTime = pauseTime + extraTime;
        endTime.postln;
        t = 0;

        0.1.wait;

        path1 = s.server.prepareForRecord(
            path +/+
            (inst.id + "-" + inst.name) ++
            "." ++ (s.server.recHeaderFormat)
        );

        0.1.wait;
        s.server.record;
        0.5.wait;
        t = 0;

        { s.play(LNX_Protocols.latency, s.beat); }.defer(0);

        while ({t < pauseTime}, {
            t = t+s.absTime;
            s.absTime.wait;
        });

        { s.pause; }.defer(0);

        while ({t < endTime}, {
            t = t+s.absTime;
            s.absTime.wait;
        });

        s.server.stopRecording;

        i = i+1;

        { s.quickLoad }.defer(0);
        
        if (i < n) {
            0.1.wait;
            while ({ s.isLoading }) { 0.1.wait; };
            this.exportNext(path, i, n, extraTime);
        }
    }
}