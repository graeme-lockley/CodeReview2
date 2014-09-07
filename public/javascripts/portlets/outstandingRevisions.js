$(document).ready(function () {
    $(function () {
        var RevisionLineView = Backbone.View.extend({
            events: {
                "click .revisionLogMessage": "selectRevision",
                "click .repoName": "selectRepo"
            },
            selectRevision: function (event) {
                console.log("/revisions/" + this.model.id + "/html");
                window.location = ("http://localhost:9000/revisions/" + this.model.id + "/html");
                return false;
            },
            selectRepo: function(event) {
                window.location = ("http://localhost:9000/repos/" + this.model.repo().id);
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
                this.$el.html(_.template(templateOnName("portlets/outstandingRevisions/main.html"), {revisions: this.model, prefix: prefix, showFeedback: this.showFeedback}));

                this.model.forEach(function (revision) {
                    var x = new RevisionLineView({model: revision, el: "#" + prefix + "-" + revision.id});
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