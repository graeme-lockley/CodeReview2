$(document).ready(function () {
    $(function () {
        var FeedbackItemView = Backbone.View.extend({
            events: {
                "click": "select"
            },
            select: function(event) {
                window.location = ("/revisionEntries/diff/" + this.model.revisionEntry().id + "/" + this.model.lineNumber());
                return false;
            }
        });

        var RevisionEntryView = Backbone.View.extend({
            initialize: function (options) {
                this.model.feedback().forEach(function (feedbackItem) {
                    var x = new FeedbackItemView({model: feedbackItem, el: "#" + options.prefix + "-" + feedbackItem.id});
                });
            },
            events: {
                "click .revisionEntryPath": "selectPath"
            },
            selectPath: function () {
                window.location = ("/revisionEntries/diff/" + this.model.id);
                return false;
            }
        });

        var RevisionLineView = Backbone.View.extend({
            initialize: function (options) {
                if (options.showFeedback) {
                    this.model.revisionEntries().forEach(function (entry) {
                        var x = new RevisionEntryView({model: entry, el: "#" + options.prefix + "-" + entry.id, prefix: options.prefix});
                    });
                }
            },
            events: {
                "click .revisionLogMessage": "selectRevision",
                "click .repoName": "selectRepo"
            },
            selectRevision: function (event) {
                window.location = ("/revisions/" + this.model.id + "/html");
                return false;
            },
            selectRepo: function (event) {
                window.location = ("/repos/" + this.model.repo().id);
                return false;
            }
        });

        var OutstandingRevisionsView = Backbone.View.extend({
            initialize: function () {
                this.showFeedback = (this.$el.attr("data-show-feedback") || "false") == "true";
                this.listenTo(this.model, 'sync', this.render);
                this.model.fetch();
            },
            render: function () {
                var prefix = this.$el.attr("id");
                var showFeedback = this.showFeedback;

                this.$el.html(_.template(templateOnName("portlets/outstandingRevisions/main.html"), {revisions: this.model, prefix: prefix, showFeedback: this.showFeedback}));

                this.model.forEach(function (revision) {
                    var x = new RevisionLineView({model: revision, el: "#" + prefix + "-" + revision.id, prefix: prefix, showFeedback: showFeedback});
                });

                return this;
            }
        });

        $(".revisions").each(function (idx, dom) {
            var revisions = new Revisions();
            revisions.url = $(dom).attr("data-collection");
            var outstandingRevisionsView = new OutstandingRevisionsView({model: revisions, el: "#" + $(dom).attr("id")});
        });
    });
});